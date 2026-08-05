package com.puma.videomax.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

@Composable
fun screenGradient(): Brush {
	val accent = LocalAccentColor.current
	val surface = MaterialTheme.colorScheme.surface
	val tinted = Color(
		red = surface.red * (1f - 0.15f) + accent.red * 0.15f,
		green = surface.green * (1f - 0.15f) + accent.green * 0.15f,
		blue = surface.blue * (1f - 0.15f) + accent.blue * 0.15f,
		alpha = 1f
	)
	return Brush.verticalGradient(
		colors = listOf(tinted, MaterialTheme.colorScheme.surfaceContainerLow)
	)
}

@Composable
fun scaffoldTintColor(): Color {
	val primary = MaterialTheme.colorScheme.primary
	return Color(primary.red, primary.green, primary.blue, 0.10f)
		.compositeOver(MaterialTheme.colorScheme.surface)
}

@Composable
fun ScreenContainer(
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface),
		content = content
	)
}
