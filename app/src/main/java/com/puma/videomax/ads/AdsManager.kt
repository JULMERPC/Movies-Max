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
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdsManager"

@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager
) {

    private var consentPendingLoad = false

    fun onConsentReady() {
        if (consentPendingLoad) {
            consentPendingLoad = false
            preloadHomeAds()
        }
    }

    // ── Interstitial ──────────────────────────────────────────────

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    fun loadInterstitial(onLoaded: (() -> Unit)? = null) {
        if (!consentManager.canRequestAds.value) {
            Log.d(TAG, "Interstitial blocked: consent not granted yet")
            consentPendingLoad = true
            return
        }
        if (interstitialAd != null || interstitialLoading) return
        interstitialLoading = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdConfig.interstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoading = false
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    interstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onDismissed: (() -> Unit)? = null
    ) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed?.invoke()
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismissed?.invoke()
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed?.invoke()
                loadInterstitial()
            }
        }
        ad.show(activity)
        interstitialAd = null
    }

    // ── Rewarded ──────────────────────────────────────────────────

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    fun loadRewarded(onLoaded: (() -> Unit)? = null) {
        if (!consentManager.canRequestAds.value) {
            Log.d(TAG, "Rewarded blocked: consent not granted yet")
            consentPendingLoad = true
            return
        }
        if (rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdConfig.rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedLoading = false
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    rewardedLoading = false
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onUserEarned: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed?.invoke()
            loadRewarded()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed?.invoke()
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onDismissed?.invoke()
                loadRewarded()
            }
        }
        ad.show(activity) { onUserEarned?.invoke() }
        rewardedAd = null
    }

    // ── Preload helpers (call from Application or Home) ───────────

    fun preloadHomeAds() {
        loadInterstitial()
        loadRewarded()
    }
}
