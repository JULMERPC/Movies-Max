package com.puma.videomax.ads

import org.junit.Assert.assertEquals
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
    fun `banner ad unit ID is not empty`() {
        assertNotNull(AdConfig.bannerAdUnitId)
        assertTrue(AdConfig.bannerAdUnitId.isNotEmpty())
    }

    @Test
    fun `banner ad unit ID has correct format`() {
        assertTrue(AdConfig.bannerAdUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun `interstitial ad unit ID is not empty`() {
        assertTrue(AdConfig.interstitialAdUnitId.isNotEmpty())
    }

    @Test
    fun `rewarded ad unit ID is not empty`() {
        assertTrue(AdConfig.rewardedAdUnitId.isNotEmpty())
    }

    @Test
    fun `app open ad unit ID is not empty`() {
        assertTrue(AdConfig.appOpenAdUnitId.isNotEmpty())
    }

    @Test
    fun `native ad unit ID has correct format`() {
        assertTrue(AdConfig.nativeAdUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun `banner ID matches the active mode`() {
        val expected = if (AdConfig.useTestAds) {
            "ca-app-pub-3940256099942544/6300978111"
        } else {
            "ca-app-pub-7120145882116895/6968706325"
        }
        assertEquals(expected, AdConfig.bannerAdUnitId)
    }

    @Test
    fun `all IDs use the publisher of the active mode`() {
        val publisherId = if (AdConfig.useTestAds) "3940256099942544" else "7120145882116895"
        listOf(
            AdConfig.bannerAdUnitId,
            AdConfig.interstitialAdUnitId,
            AdConfig.rewardedAdUnitId,
            AdConfig.nativeAdUnitId,
            AdConfig.appOpenAdUnitId
        ).forEach { id ->
            assertTrue("ID $id should belong to publisher $publisherId", id.contains(publisherId))
        }
    }
}
