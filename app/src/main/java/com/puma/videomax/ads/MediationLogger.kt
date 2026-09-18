package com.puma.videomax.ads

import android.util.Log
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.ResponseInfo
import com.puma.videomax.BuildConfig

/**
 * DEBUG-only mediation diagnostics: which network won the auction for each
 * loaded ad (AdMob / Unity Ads / AppLovin) and the full waterfall/bidding
 * chain with per-source latency. No-op in release builds.
 *
 * Read with:  adb logcat -s Mediation
 * Winner line looks like:
 *   Mediation: LOADED interstitial via com.google.ads.mediation.unity.UnityMediationAdapter
 */
object MediationLogger {

    private const val TAG = "Mediation"

    fun logLoaded(placement: String, responseInfo: ResponseInfo?) {
        if (!BuildConfig.DEBUG) return
        if (responseInfo == null) {
            Log.d(TAG, "LOADED $placement via unknown (no ResponseInfo)")
            return
        }
        Log.d(TAG, "LOADED $placement via ${responseInfo.mediationAdapterClassName}")
        responseInfo.adapterResponses.forEach { logAdapterResponse(it) }
    }

    fun logInitStatus(adapterStatuses: Map<String, com.google.android.gms.ads.initialization.AdapterStatus>) {
        if (!BuildConfig.DEBUG) return
        adapterStatuses.forEach { (name, status) ->
            Log.d(
                TAG,
                "INIT $name state=${status.initializationState} " +
                    "latency=${status.latency}ms desc='${status.description}'"
            )
        }
    }

    private fun logAdapterResponse(info: AdapterResponseInfo) {
        Log.d(
            TAG,
            "  ↳ source=${info.adSourceName} " +
                "id=${info.adSourceId} " +
                "instance=${info.adSourceInstanceName} " +
                "latency=${info.latencyMillis}ms " +
                "error=${info.adError?.message ?: "none"}"
        )
    }
}
