package com.example.videomax.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Standard screen gradient used across Library, Settings, Playlists, etc.
 */
@Composable
fun screenGradient(): Brush = Brush.verticalGradient(
	colors = listOf(
		MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
		MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
		MaterialTheme.colorScheme.background
	)
)

/**
 * Standard screen container with the app gradient background.
 */
@Composable
fun ScreenContainer(
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(screenGradient()),
		content = content
	)
}
