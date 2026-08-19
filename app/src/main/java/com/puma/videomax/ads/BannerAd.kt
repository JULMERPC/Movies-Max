package com.puma.videomax.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAd"

@Composable
fun BannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val consentManager = LocalConsentManager.current
    val canRequestAds = consentManager.canRequestAds.collectAsState()

    if (!canRequestAds.value) return

    val adUnitId = AdConfig.bannerAdUnitId

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdConfig.getBannerAdSize(context))
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Banner ad failed to load: ${error.message}")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner ad opened")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner ad closed")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        update = { }
    )
}
