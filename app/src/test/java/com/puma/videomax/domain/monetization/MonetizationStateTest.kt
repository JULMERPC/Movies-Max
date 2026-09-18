package com.puma.videomax.domain.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationStateTest {

    @Test
    fun `fresh state shows ads`() {
        val state = MonetizationState()
        assertFalse(state.areAdsRemoved(nowMs = 1_000L))
        assertFalse(state.hasAdFreePass(nowMs = 1_000L))
    }

    @Test
    fun `premium removes ads permanently`() {
        val state = MonetizationState(isPremium = true)
        assertTrue(state.areAdsRemoved(nowMs = Long.MAX_VALUE))
    }

    @Test
    fun `ad-free pass is valid before expiry`() {
        val state = MonetizationState(adFreeUntilMs = 2_000L)
        assertTrue(state.hasAdFreePass(nowMs = 1_999L))
        assertTrue(state.areAdsRemoved(nowMs = 1_999L))
    }

    @Test
    fun `ad-free pass expires on time`() {
        val state = MonetizationState(adFreeUntilMs = 2_000L)
        assertFalse(state.hasAdFreePass(nowMs = 2_000L))
        assertFalse(state.areAdsRemoved(nowMs = 2_001L))
    }
}
