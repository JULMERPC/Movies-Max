package com.puma.videomax.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "PlayBillingDS"

/**
 * Play Billing implementation of "Remove Ads" as a one-time purchase.
 *
 * Setup required in Play Console (one time, ~10 min):
 * 1. Monetize > Products > In-app products > Create product,
 *    Product ID = [REMOVE_ADS_PRODUCT_ID] ("remove_ads"), one-time.
 * 2. Set price, save, activate. Purchases only work in builds signed
 *    with the upload key (internal testing track or later).
 *
 * Flow: connect → query owned purchases (restore) → query price for
 * Settings → launch flow → acknowledge → emit [premiumUpdates].
 * The repository persists the flag; entitlement survives reinstalls
 * because Play remembers the purchase.
 */
@Singleton
class PlayBillingPremiumDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : PremiumDataSource, PurchasesUpdatedListener {

    companion object {
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
        private const val RESTORE_RETRY_ATTEMPTS = 3
        private const val RESTORE_RETRY_DELAY_MS = 1_200L
    }

    private val _premiumUpdates = MutableStateFlow(false)
    override val premiumUpdates: StateFlow<Boolean> = _premiumUpdates.asStateFlow()

    private var cachedDetails: ProductDetails? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        // Sincronización en inicio: al conectar con Play se re-consultan las
        // compras poseídas para que el estado Premium sobreviva reinicios y
        // reconexiones sin pedir un pago nuevo.
        connect {
            scope.launch { runCatching { queryOwnedRemoveAds() } }
        }
    }

    // ── PremiumDataSource ─────────────────────────────────────────

    override suspend fun isRemoveAdsPurchased(): Boolean {
        if (!ensureConnected()) return _premiumUpdates.value
        return queryOwnedRemoveAds()
    }

    override suspend fun launchRemoveAdsPurchase(activity: Activity): Boolean {
        if (!ensureConnected()) {
            Log.w(TAG, "launch failed: not connected to Play (Play Store ausente/sin red?)")
            return false
        }
        val details = cachedDetails ?: queryDetails() ?: run {
            Log.w(TAG, "launch failed: sin ProductDetails para '$REMOVE_ADS_PRODUCT_ID'. Causas: producto inactivo/sin propagar en Console, app no instalada desde Play, o cuenta sin licencia.")
            return false
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "launchBillingFlow OK: ventana de pago entregada a Play")
            return true
        }
        // "Ya compraste este elemento": no es un error para el usuario, es la
        // señal de que la compra existe en Play. Se restaura sin pedir pago nuevo.
        // Con retry: el caché de Play puede tardar en reflejar la compra justo
        // después del pago o tras reinstalar.
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.i(TAG, "launchBillingFlow ITEM_ALREADY_OWNED: restaurando compra existente")
            return restorePurchasesWithRetry()
        }
        Log.w(TAG, "launchBillingFlow FAILED: code=${result.responseCode} (${codeName(result.responseCode)}) msg=${result.debugMessage}")
        return false
    }

    override suspend fun restorePurchases(): Boolean {
        if (!ensureConnected()) return _premiumUpdates.value
        return queryOwnedRemoveAds()
    }

    override suspend fun hasPendingRemoveAdsPurchase(): Boolean {
        if (!ensureConnected()) return false
        val (result, purchases) = queryPurchases()
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return false
        return purchases.any {
            it.products.contains(REMOVE_ADS_PRODUCT_ID) &&
                it.purchaseState == Purchase.PurchaseState.PENDING
        }
    }

    /**
     * Restore con reintentos: solo para el camino ITEM_ALREADY_OWNED, donde
     * Play afirma que la compra existe pero su caché aún no la devuelve en
     * queryPurchases. El arranque en frío NO reintenta (el "no comprado" normal
     * debe resolver rápido sin penalizar a quien nunca pagó).
     */
    private suspend fun restorePurchasesWithRetry(
        attempts: Int = RESTORE_RETRY_ATTEMPTS,
        delayMs: Long = RESTORE_RETRY_DELAY_MS
    ): Boolean {
        repeat(attempts) { attempt ->
            if (!ensureConnected()) return _premiumUpdates.value
            if (queryOwnedRemoveAds()) return true
            // Si hay un pago pendiente, reintentar es inútil: solo Play puede
            // completarlo. Se informa al usuario en vez de seguir girando.
            if (hasPendingRemoveAdsPurchase()) {
                Log.i(TAG, "restore: compra en estado PENDING, se requiere completar el pago en Play")
                return false
            }
            if (attempt < attempts - 1) {
                Log.d(TAG, "restore: intento ${attempt + 1}/$attempts sin compra visible, reintentando...")
                delay(delayMs)
            }
        }
        Log.w(TAG, "restore: ITEM_ALREADY_OWNED pero queryPurchases no devuelve la compra tras $attempts intentos (cuenta distinta en Play, o caché desactualizado)")
        return _premiumUpdates.value
    }

    override suspend fun getRemoveAdsPrice(): String? {
        if (cachedDetails == null && ensureConnected()) {
            queryDetails()
        }
        return cachedDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    // ── PurchasesUpdatedListener ──────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        // ITEM_ALREADY_OWNED llega cuando el usuario ya posee el INAPP: se
        // restaura el estado Pro en vez de mostrar un error genérico.
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.i(TAG, "onPurchasesUpdated ITEM_ALREADY_OWNED: restaurando compra existente")
            scope.launch { runCatching { restorePurchasesWithRetry() } }
            return
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            // USER_CANCELED(1) es normal (cerró la ventana); el resto se loguea.
            Log.d(TAG, "onPurchasesUpdated: code=${result.responseCode} (${codeName(result.responseCode)})")
            return
        }
        if (purchases == null) return
        purchases
            .filter { it.products.contains(REMOVE_ADS_PRODUCT_ID) }
            .forEach { handlePurchase(it) }
    }

    // ── Internals ─────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "handlePurchase: '${purchase.orderId}' en PENDING (pago sin completar en Play, no otorga Premium ni se reconoce)")
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) {
            _premiumUpdates.value = true
            return
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            Log.d(TAG, "acknowledge: ${result.responseCode}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _premiumUpdates.value = true
            }
        }
    }

    private suspend fun queryOwnedRemoveAds(): Boolean {
        val (result, purchases) = queryPurchases()
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "queryOwnedRemoveAds: code=${result.responseCode} (${codeName(result.responseCode)})")
            return _premiumUpdates.value
        }
        val relevant = purchases.filter { it.products.contains(REMOVE_ADS_PRODUCT_ID) }
        Log.d(TAG, "queryOwnedRemoveAds: ${relevant.size} compra(s) de '$REMOVE_ADS_PRODUCT_ID' " +
            relevant.joinToString { "state=${it.purchaseState} ack=${it.isAcknowledged}" })
        val owned = relevant.any {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        relevant.forEach { handlePurchase(it) }
        if (owned) _premiumUpdates.value = true
        return owned
    }

    private suspend fun queryDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val (result, details) = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                if (!cont.isCompleted) cont.resume(billingResult to productDetailsResult.productDetailsList)
            }
        }
        if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
            Log.d(TAG, "queryProductDetails OK: '${details.first().productId}' ${details.first().oneTimePurchaseOfferDetails?.formattedPrice}")
        } else {
            Log.w(TAG, "queryProductDetails: code=${result.responseCode} (${codeName(result.responseCode)}) count=${details.size} para '$REMOVE_ADS_PRODUCT_ID'. Lista vacía = producto inactivo/sin propagar, ID distinto, o app no instalada desde Play.")
        }
        cachedDetails = details.firstOrNull()
        return cachedDetails
    }

    private suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (!cont.isCompleted) cont.resume(billingResult to purchases)
            }
        }
    }

    private fun connect(onConnected: (() -> Unit)? = null) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "setup finished: OK")
                } else {
                    Log.w(TAG, "setup finished FAILED: code=${result.responseCode} (${codeName(result.responseCode)}) msg=${result.debugMessage}")
                }
                onConnected?.invoke()
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "service disconnected (library retries automatically)")
            }
        })
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        val result = suspendCancellableCoroutine<BillingResult> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (!cont.isCompleted) cont.resume(billingResult)
                }

                override fun onBillingServiceDisconnected() {
                    // Library retries automatically; launch/query will fail fast below.
                }
            })
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "ensureConnected FAILED: code=${result.responseCode} (${codeName(result.responseCode)}) msg=${result.debugMessage}")
        }
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Human-readable Billing response codes for logcat diagnosis. */
    private fun codeName(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "SERVICE_TIMEOUT"
        else -> "UNKNOWN"
    }
}
