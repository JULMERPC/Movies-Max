package com.puma.videomax.ads

import androidx.compose.runtime.staticCompositionLocalOf

val LocalConsentManager = staticCompositionLocalOf<ConsentManager> {
    error("No ConsentManager provided")
}
