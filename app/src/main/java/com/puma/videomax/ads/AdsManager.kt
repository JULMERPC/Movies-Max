package com.puma.videomax.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.puma.videomax.domain.monetization.MonetizationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdsManager"

/**
 * Loads and shows full-screen ads. It holds NO business logic:
 * every "should I load/show?" decision is delegated to [AdController],
 * which enforces consent, Premium, the 24h ad-free pass and the
 * 12-minute interstitial frequency cap.
 *
 * Interstitials are only ever shown from [showInterstitialIfAllowed],
 * called on natural section transitions — never inside continuous
 * actions like pressing Next.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val adController: AdController,
    private val monetization: MonetizationRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Interstitial ─────────────────────────────────────────────

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    fun loadInterstitial(onLoaded: (() -> Unit)? = null) {
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial already loaded")
            onLoaded?.invoke()
            return
        }
        if (interstitialLoading) {
            Log.d(TAG, "Interstitial already loading, skipping")
            return
        }
        interstitialLoading = true
        scope.launch {
            try {
                waitForConsent()
                if (!adController.canRequestAds(AdPlacement.INTERSTITIAL_TRANSITION)) {
                    Log.d(TAG, "Interstitial: ads removed (Premium/pass), skipping load")
                    interstitialLoading = false
                    return@launch
                }
                Log.d(TAG, "Interstitial: loading ad...")
                val request = AdRequest.Builder().build()
                InterstitialAd.load(
                    context,
                    AdConfig.interstitialAdUnitId,
                    request,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Interstitial loaded successfully")
                            MediationLogger.logLoaded("interstitial", ad.responseInfo)
                            interstitialAd = ad
                            interstitialLoading = false
                            onLoaded?.invoke()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Interstitial failed to load: code=${error.code}, message=${error.message}")
                            interstitialAd = null
                            interstitialLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Interstitial load exception", e)
                interstitialLoading = false
            }
        }
    }

    /**
     * Shows an interstitial ONLY if [AdController.canShowInterstitialNow]
     * allows it (consent + no Premium/pass + 12-min cap) and an ad is
     * loaded. Otherwise [onDismissed] runs immediately so navigation
     * is never blocked. The display timestamp is recorded for the cap.
     */
    fun showInterstitialIfAllowed(activity: Activity, onDismissed: (() -> Unit)? = null) {
        if (!adController.canShowInterstitialNow()) {
            Log.d(TAG, "showInterstitialIfAllowed: blocked by AdController, continuing")
            onDismissed?.invoke()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "showInterstitialIfAllowed: no ad ready, continuing")
            onDismissed?.invoke()
            loadInterstitial()
            return
        }
        Log.d(TAG, "showInterstitialIfAllowed: showing ad")
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
                onDismissed?.invoke()
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial show failed: ${error.message}")
                interstitialAd = null
                onDismissed?.invoke()
                loadInterstitial()
            }
        }
        scope.launch { monetization.markInterstitialShown() }
        ad.show(activity)
        interstitialAd = null
    }

    fun canShowInterstitial(): Boolean {
        if (!adController.canShowInterstitialNow()) return false
        if (interstitialAd == null) {
            Log.d(TAG, "Interstitial: no ad loaded")
            return false
        }
        Log.d(TAG, "Interstitial: ready to show")
        return true
    }

    // ── Rewarded ─────────────────────────────────────────────────

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    fun loadRewarded(onLoaded: (() -> Unit)? = null) {
        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded already loaded")
            onLoaded?.invoke()
            return
        }
        if (rewardedLoading) {
            Log.d(TAG, "Rewarded already loading, skipping")
            return
        }
        rewardedLoading = true
        scope.launch {
            try {
                waitForConsent()
                if (!adController.canRequestAds(AdPlacement.REWARDED)) {
                    Log.d(TAG, "Rewarded: cannot request ads, skipping load")
                    rewardedLoading = false
                    return@launch
                }
                Log.d(TAG, "Rewarded: loading ad...")
                val request = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    AdConfig.rewardedAdUnitId,
                    request,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.d(TAG, "Rewarded loaded successfully")
                            MediationLogger.logLoaded("rewarded", ad.responseInfo)
                            rewardedAd = ad
                            rewardedLoading = false
                            onLoaded?.invoke()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Rewarded failed to load: code=${error.code}, message=${error.message}")
                            rewardedAd = null
                            rewardedLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Rewarded load exception", e)
                rewardedLoading = false
            }
        }
    }

    fun showRewarded(
        activity: Activity,
        onUserEarned: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "showRewarded: no ad ready")
            onDismissed?.invoke()
            loadRewarded()
            return
        }
        Log.d(TAG, "showRewarded: showing ad")
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded dismissed")
                rewardedAd = null
                onDismissed?.invoke()
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded show failed: ${error.message}")
                rewardedAd = null
                onDismissed?.invoke()
                loadRewarded()
            }
        }
        ad.show(activity) { onUserEarned?.invoke() }
        rewardedAd = null
    }

    // ── Consent ──────────────────────────────────────────────────

    fun onConsentReady() {
        Log.d(TAG, "Consent ready, preloading ads - canRequestAds=${consentManager.canRequestAds.value}")
        preloadHomeAds()
    }

    fun preloadHomeAds() {
        Log.d(TAG, "preloadHomeAds called")
        loadInterstitial()
        loadRewarded()
    }

    private suspend fun waitForConsent() {
        if (consentManager.canRequestAds.value) {
            Log.d(TAG, "waitForConsent: already have consent")
            return
        }
        Log.d(TAG, "waitForConsent: waiting for consent...")
        consentManager.canRequestAds.filter { it }.first()
        Log.d(TAG, "waitForConsent: consent granted")
    }
}
