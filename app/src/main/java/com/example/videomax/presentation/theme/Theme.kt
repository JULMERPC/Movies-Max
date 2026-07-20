package com.example.videomax.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.videomax.domain.model.ThemeMode

private val TealPrimary = Color(0xFF006A6A)
private val TealOnPrimary = Color(0xFFFFFFFF)
private val TealContainer = Color(0xFF6FF7F6)
private val TealOnContainer = Color(0xFF002020)

private val CoralSecondary = Color(0xFF9A4522)
private val CoralContainer = Color(0xFFFFDBCF)

private val LightColors = lightColorScheme(
	primary = TealPrimary,
	onPrimary = TealOnPrimary,
	primaryContainer = TealContainer,
	onPrimaryContainer = TealOnContainer,
	secondary = CoralSecondary,
	secondaryContainer = CoralContainer,
	background = Color(0xFFF5FBFA),
	surface = Color(0xFFF5FBFA),
	surfaceVariant = Color(0xFFDAE5E4),
	onSurface = Color(0xFF161D1D),
	onSurfaceVariant = Color(0xFF3F4948)
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFF4CDAD9),
	onPrimary = Color(0xFF003738),
	primaryContainer = Color(0xFF004F50),
	onPrimaryContainer = Color(0xFF6FF7F6),
	secondary = Color(0xFFFFB59A),
	secondaryContainer = Color(0xFF7C2E0D),
	background = Color(0xFF0E1515),
	surface = Color(0xFF0E1515),
	surfaceVariant = Color(0xFF3F4948),
	onSurface = Color(0xFFDDE4E3),
	onSurfaceVariant = Color(0xFFBEC9C8)
)

@Composable
fun VideoPlayerProTheme(
	themeMode: ThemeMode = ThemeMode.SYSTEM,
	content: @Composable () -> Unit
) {
	val darkTheme = when (themeMode) {
		ThemeMode.LIGHT -> false
		ThemeMode.DARK -> true
		ThemeMode.SYSTEM -> isSystemInDarkTheme()
	}

	val colors: ColorScheme = if (darkTheme) DarkColors else LightColors

	MaterialTheme(
		colorScheme = colors,
		typography = Typography,
		content = content
	)
}
