package com.puma.videomax.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdSize
import com.puma.videomax.BuildConfig

object AdConfig {

    const val APPLICATION_ID = "ca-app-pub-7120145882116895~3435821277"

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    private const val PRODUCTION_BANNER = "ca-app-pub-7120145882116895/6968706325"
    private const val PRODUCTION_INTERSTITIAL = "ca-app-pub-7120145882116895/7221771000"
    private const val PRODUCTION_REWARDED = "ca-app-pub-7120145882116895/9730601032"
    private const val PRODUCTION_NATIVE = "ca-app-pub-7120145882116895/8888826456"
    private const val PRODUCTION_APP_OPEN = "ca-app-pub-7120145882116895/4784873029"

    /** Test ads in debug, real production IDs in release. */
    val useTestAds: Boolean get() = BuildConfig.DEBUG

    fun init(context: Context) {
        // Reserved for future use
    }

    fun getBannerAdSize(context: Context): AdSize {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt()
        )
    }

    val bannerAdUnitId: String
        get() = if (useTestAds) TEST_BANNER else PRODUCTION_BANNER

    val interstitialAdUnitId: String
        get() = if (useTestAds) TEST_INTERSTITIAL else PRODUCTION_INTERSTITIAL

    val rewardedAdUnitId: String
        get() = if (useTestAds) TEST_REWARDED else PRODUCTION_REWARDED

    val nativeAdUnitId: String
        get() = if (useTestAds) TEST_NATIVE else PRODUCTION_NATIVE

    val appOpenAdUnitId: String
        get() = if (useTestAds) TEST_APP_OPEN else PRODUCTION_APP_OPEN
}
