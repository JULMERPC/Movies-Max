package com.puma.videomax.domain.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdPlacerTest {

	@Test
	fun `empty list has no slots`() {
		assertEquals(0, NativeAdPlacer.totalWithAds(0))
	}

	@Test
	fun `no ad when fewer items than interval`() {
		assertEquals(10, NativeAdPlacer.totalWithAds(10))
		for (pos in 0 until 10) {
			assertFalse(NativeAdPlacer.isAdPosition(pos))
		}
	}

	@Test
	fun `one ad after eleven content items`() {
		assertEquals(12, NativeAdPlacer.totalWithAds(11))
		assertTrue(NativeAdPlacer.isAdPosition(11))
		assertFalse(NativeAdPlacer.isAdPosition(10))
		assertFalse(NativeAdPlacer.isAdPosition(12))
	}

	@Test
	fun `content indexes skip ad positions`() {
		assertEquals(0, NativeAdPlacer.contentIndexFor(0))
		assertEquals(10, NativeAdPlacer.contentIndexFor(10))
		// position 11 is the ad; next content resumes at 11
		assertEquals(11, NativeAdPlacer.contentIndexFor(12))
		assertEquals(22, NativeAdPlacer.contentIndexFor(24))
	}

	@Test
	fun `two ads for 22 items`() {
		assertEquals(24, NativeAdPlacer.totalWithAds(22))
		assertTrue(NativeAdPlacer.isAdPosition(11))
		assertTrue(NativeAdPlacer.isAdPosition(23))
	}
}
