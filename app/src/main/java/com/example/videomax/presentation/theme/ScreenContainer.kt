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
import androidx.compose.ui.graphics.compositeOver

@Composable
fun screenGradient(): Brush = Brush.verticalGradient(
	colors = listOf(
		MaterialTheme.colorScheme.surface,
		MaterialTheme.colorScheme.surfaceContainerLow
	)
)

@Composable
fun scaffoldTintColor(): Color {
	val primary = MaterialTheme.colorScheme.primary
	return Color(primary.red, primary.green, primary.blue, 0.10f)
		.compositeOver(Color.White)
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
