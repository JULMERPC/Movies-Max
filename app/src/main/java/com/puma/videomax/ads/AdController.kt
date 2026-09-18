package com.puma.videomax.ads

import com.puma.videomax.domain.monetization.MonetizationRepository
import com.puma.videomax.domain.monetization.MonetizationRepository.Companion.APP_OPEN_MIN_BACKGROUND_MS
import com.puma.videomax.domain.monetization.MonetizationRepository.Companion.INTERSTITIAL_MIN_GAP_MS
import javax.inject.Inject
import javax.inject.Singleton

enum class AdPlacement {
    NATIVE_LIST,
    INTERSTITIAL_TRANSITION,
    REWARDED,
    APP_OPEN
}

/**
 * Single decision gate for every ad in the app.
 *
 * Order of checks (cheapest first):
 * 1. UMP consent ([ConsentManager.canRequestAds]).
 * 2. Premium / 24h ad-free pass ([MonetizationRepository]).
 *    Rewarded ads are always allowed: they are the path to earn the pass.
 * 3. Placement-specific rules: interstitial frequency cap (12 min),
 *    App Open background threshold (4h).
 *
 * No screen or manager should call Mobile Ads directly without passing here.
 */
@Singleton
class AdController @Inject constructor(
    private val monetization: MonetizationRepository,
    private val consentManager: ConsentManager
) {
    fun canRequestAds(placement: AdPlacement): Boolean {
        if (!consentManager.canRequestAds.value) return false
        if (placement == AdPlacement.REWARDED) return true
        return !monetization.state.value.areAdsRemoved()
    }

    /**
     * Interstitials are only allowed on natural section transitions —
     * never inside continuous actions (e.g. pressing Next repeatedly).
     */
    fun canShowInterstitialNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!canRequestAds(AdPlacement.INTERSTITIAL_TRANSITION)) return false
        val last = monetization.state.value.lastInterstitialShownAtMs
        return (nowMs - last) >= INTERSTITIAL_MIN_GAP_MS
    }

    fun canShowAppOpen(backgroundedForMs: Long): Boolean {
        if (!canRequestAds(AdPlacement.APP_OPEN)) return false
        return backgroundedForMs >= APP_OPEN_MIN_BACKGROUND_MS
    }
}
