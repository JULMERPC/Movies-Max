package com.puma.videomax.ads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test

class BannerAdTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bannerAd_renders_without_crash() {
        composeTestRule.setContent {
            BannerAd()
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun bannerAd_with_modifier_renders_without_crash() {
        composeTestRule.setContent {
            BannerAd(
                modifier = androidx.compose.foundation.layout.fillMaxWidth()
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
