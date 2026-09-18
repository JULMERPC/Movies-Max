package com.puma.videomax.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportDeveloperFormatTest {

	@Test
	fun `formats hours and minutes`() {
		assertEquals("23h 45m", formatPassRemaining((23 * 60 + 45) * 60_000L))
	}

	@Test
	fun `formats exact hours without minutes`() {
		assertEquals("2h", formatPassRemaining(2 * 60 * 60_000L))
	}

	@Test
	fun `formats minutes only`() {
		assertEquals("45m", formatPassRemaining(45 * 60_000L))
	}

	@Test
	fun `zero or negative shows less than a minute`() {
		assertEquals("menos de 1 min", formatPassRemaining(0L))
		assertEquals("menos de 1 min", formatPassRemaining(-1L))
		assertEquals("menos de 1 min", formatPassRemaining(30_000L))
	}

	@Test
	fun `full 24h pass formats correctly`() {
		assertEquals("24h", formatPassRemaining(24 * 60 * 60_000L))
	}
}
