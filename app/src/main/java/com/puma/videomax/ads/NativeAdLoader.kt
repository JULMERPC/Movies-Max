package com.puma.videomax.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "NativeAdLoader"

/**
 * Loads a single AdMob Native Advanced ad.
 *
 * Gating (via [AdController]): UMP consent + no Premium/24h pass.
 * A blank ad unit ID (production native not configured yet) resolves
 * to null so lists render clean. The caller owns the returned [NativeAd]
 * and must call `destroy()` when the slot leaves the composition.
 */
@Singleton
class NativeAdLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val adController: AdController
) {
    suspend fun load(): NativeAd? {
        if (!consentManager.canRequestAds.value) {
            Log.d(TAG, "Waiting for consent...")
            consentManager.canRequestAds.filter { it }.first()
        }
        if (!adController.canRequestAds(AdPlacement.NATIVE_LIST)) {
            Log.d(TAG, "Native load skipped: ads removed (Premium/pass)")
            return null
        }
        val adUnitId = AdConfig.nativeAdUnitId
        if (adUnitId.isBlank()) {
            Log.d(TAG, "Native load skipped: no ad unit configured")
            return null
        }
        return suspendCancellableCoroutine { cont ->
            val loader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad ->
                    Log.d(TAG, "Native loaded successfully")
                    MediationLogger.logLoaded("native", ad.responseInfo)
                    if (!cont.isCompleted) cont.resume(ad)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native failed: code=${error.code} ${error.message}")
                        if (!cont.isCompleted) cont.resume(null)
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setRequestCustomMuteThisAd(true)
                        .build()
                )
                .build()
            loader.loadAd(AdRequest.Builder().build())
        }
    }
}
