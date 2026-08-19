package com.puma.videomax.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigTest {

    @Test
    fun `application ID is not empty`() {
        assertTrue(AdConfig.APPLICATION_ID.isNotEmpty())
    }

    @Test
    fun `application ID matches expected format`() {
        assertTrue(AdConfig.APPLICATION_ID.startsWith("ca-app-pub-"))
    }

    @Test
    fun `useTestAds defaults to false without init`() {
        assertFalse(AdConfig.useTestAds)
    }

    @Test
    fun `banner ad unit ID is not empty`() {
        assertNotNull(AdConfig.bannerAdUnitId)
        assertTrue(AdConfig.bannerAdUnitId.isNotEmpty())
    }

    @Test
    fun `banner ad unit ID has correct format`() {
        assertTrue(AdConfig.bannerAdUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun `rewarded ad unit ID is not empty`() {
        assertTrue(AdConfig.rewardedAdUnitId.isNotEmpty())
    }

    @Test
    fun `rewarded ad unit ID has correct format`() {
        assertTrue(AdConfig.rewardedAdUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun `production banner ID matches expected`() {
        assertEquals("ca-app-pub-7120145882116895/6968706325", AdConfig.bannerAdUnitId)
    }

    @Test
    fun `production rewarded ID matches expected`() {
        assertEquals("ca-app-pub-7120145882116895/9730601032", AdConfig.rewardedAdUnitId)
    }

    @Test
    fun `production IDs use the correct publisher ID`() {
        val productionPublisherId = "7120145882116895"
        assertTrue(AdConfig.bannerAdUnitId.contains(productionPublisherId))
        assertTrue(AdConfig.rewardedAdUnitId.contains(productionPublisherId))
    }
}
