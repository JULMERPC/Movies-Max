package com.puma.videomax.domain.monetization

/**
 * Single source of truth for the monetization state.
 *
 * - [isPremium]: permanent "Remove Ads" IAP.
 * - [adFreeUntilMs]: temporary pass earned via 2 consecutive rewarded ads (24h).
 * - [rewardedProgress]: 0..[REWARDED_REQUIRED], progress toward the next pass.
 * - [lastInterstitialShownAtMs]: timestamp of the last interstitial, used for
 *   the strict 12-15 min frequency cap.
 */
data class MonetizationState(
    val isPremium: Boolean = false,
    val adFreeUntilMs: Long = 0L,
    val rewardedProgress: Int = 0,
    val lastInterstitialShownAtMs: Long = 0L
) {
    fun hasAdFreePass(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs < adFreeUntilMs

    fun areAdsRemoved(nowMs: Long = System.currentTimeMillis()): Boolean =
        isPremium || hasAdFreePass(nowMs)
}
