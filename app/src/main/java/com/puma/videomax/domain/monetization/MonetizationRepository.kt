package com.puma.videomax.domain.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for monetization state: Premium (IAP) + temporary ad-free pass
 * (2 rewarded ads = 24h) + interstitial frequency cap bookkeeping.
 *
 * Implementations must persist state across process restarts (DataStore).
 */
interface MonetizationRepository {

    companion object {
        /** Rewarded ads required to earn one ad-free pass. */
        const val REWARDED_REQUIRED = 2

        /** Duration of the ad-free pass granted after [REWARDED_REQUIRED] rewarded ads. */
        const val AD_FREE_PASS_MS = 24 * 60 * 60 * 1000L

        /** Minimum gap between two interstitials within a session. */
        const val INTERSTITIAL_MIN_GAP_MS = 12 * 60 * 1000L

        /** App Open ads: only if the app was in background longer than this. */
        const val APP_OPEN_MIN_BACKGROUND_MS = 4 * 60 * 60 * 1000L
    }

    val state: StateFlow<MonetizationState>

    /**
     * Registers one completed rewarded ad.
     * @return true if this completion earned a new 24h ad-free pass.
     */
    suspend fun registerRewardedCompleted(): Boolean

    suspend fun setPremium(purchased: Boolean)

    suspend fun markInterstitialShown()

    /** Debug/testing only. Never expose in production UI. */
    suspend fun clearAdFreePass()
}
