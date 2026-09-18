package com.puma.videomax.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "AppOpenAdManager"

/**
 * Shows an App Open ad only when the app returns from a background stay
 * longer than [MonetizationRepository.APP_OPEN_MIN_BACKGROUND_MS] (4h).
 * Every decision is delegated to [AdController], so Premium users and
 * 24h ad-free pass holders never load nor see App Open ads.
 */
@Singleton
class AppOpenAdManager @Inject constructor(
    private val context: Application,
    private val consentManager: ConsentManager,
    private val adController: AdController
) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var lastPausedAtMs = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun register() {
        context.registerActivityLifecycleCallbacks(this)
        Log.d(TAG, "Registered lifecycle callbacks")
    }

    fun unregister() {
        context.unregisterActivityLifecycleCallbacks(this)
    }

    fun onConsentReady() {
        Log.d(TAG, "Consent ready - appOpenAd=${appOpenAd != null}, isLoading=$isLoading")
        if (appOpenAd == null && !isLoading) {
            loadAd()
        }
    }

    private fun loadAd() {
        if (appOpenAd != null || isLoading) {
            Log.d(TAG, "loadAd: skipping - appOpenAd=${appOpenAd != null}, isLoading=$isLoading")
            return
        }
        isLoading = true
        scope.launch {
            try {
                if (!consentManager.canRequestAds.value) {
                    Log.d(TAG, "Waiting for consent...")
                    consentManager.canRequestAds.filter { it }.first()
                }
                if (!adController.canRequestAds(AdPlacement.APP_OPEN)) {
                    Log.d(TAG, "loadAd: ads removed (Premium/pass), skipping load")
                    isLoading = false
                    return@launch
                }
                Log.d(TAG, "Consent obtained, loading app open ad...")
                val request = AdRequest.Builder().build()
                AppOpenAd.load(
                    context,
                    AdConfig.appOpenAdUnitId,
                    request,
                    object : AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            Log.d(TAG, "App open ad loaded successfully")
                            MediationLogger.logLoaded("app_open", ad.responseInfo)
                            appOpenAd = ad
                            isLoading = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "App open ad failed to load: code=${error.code}, message=${error.message}")
                            appOpenAd = null
                            isLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "App open load exception", e)
                isLoading = false
            }
        }
    }

    private fun showAdIfAvailable(backgroundedForMs: Long) {
        if (!adController.canShowAppOpen(backgroundedForMs)) {
            Log.d(TAG, "showAdIfAvailable: blocked by AdController (backgroundedForMs=$backgroundedForMs)")
            return
        }
        val ad = appOpenAd
        val activity = currentActivity
        if (ad == null || activity == null || isShowingAd) {
            Log.d(TAG, "showAdIfAvailable: not showing - ad=${ad != null}, activity=${activity != null}, isShowingAd=$isShowingAd")
            return
        }

        Log.d(TAG, "Showing app open ad")
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "App open show failed: ${error.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
        }
        isShowingAd = true
        ad.show(activity)
    }

    // ── ActivityLifecycleCallbacks ────────────────────────────────

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        val backgroundedForMs = if (lastPausedAtMs <= 0L) {
            Long.MAX_VALUE // Cold start: the app was closed, treat as long background stay.
        } else {
            System.currentTimeMillis() - lastPausedAtMs
        }
        showAdIfAvailable(backgroundedForMs)
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
        lastPausedAtMs = System.currentTimeMillis()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
