package com.puma.videomax.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdSize

object AdConfig {

    const val APPLICATION_ID = "ca-app-pub-7120145882116895~3435821277"

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    private const val PRODUCTION_BANNER = "ca-app-pub-7120145882116895/6968706325"
    private const val PRODUCTION_INTERSTITIAL = ""
    private const val PRODUCTION_REWARDED = "ca-app-pub-7120145882116895/9730601032"
    private const val PRODUCTION_NATIVE = ""
    private const val PRODUCTION_APP_OPEN = ""

    private var debugOverride: Boolean = false

    fun init(context: Context) {
        debugOverride = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val useTestAds: Boolean get() = debugOverride

    fun getBannerAdSize(context: Context): AdSize {
        val displayMetrics = context.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val adWidth = (adWidthPixels / displayMetrics.density).toInt()
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 90)
    }

    val bannerAdUnitId: String
        get() = if (debugOverride) TEST_BANNER else PRODUCTION_BANNER

    val interstitialAdUnitId: String
        get() = if (debugOverride) TEST_INTERSTITIAL else PRODUCTION_INTERSTITIAL

    val rewardedAdUnitId: String
        get() = if (debugOverride) TEST_REWARDED else PRODUCTION_REWARDED

    val nativeAdUnitId: String
        get() = if (debugOverride) TEST_NATIVE else PRODUCTION_NATIVE

    val appOpenAdUnitId: String
        get() = if (debugOverride) TEST_APP_OPEN else PRODUCTION_APP_OPEN
}
