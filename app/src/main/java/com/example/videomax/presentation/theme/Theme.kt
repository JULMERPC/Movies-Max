package com.example.videomax.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.videomax.domain.model.ThemeMode

private val TealPrimary = Color(0xFF2C92E5)
private val TealContainer = Color(0xFF9EF0EF)
private val CoralSecondary = Color(0xFF9D0F1A)

private val LightColors = lightColorScheme(
	primary = TealPrimary,
	onPrimary = Color.White,
	primaryContainer = TealContainer,
	onPrimaryContainer = Color(0xFF002020),
	secondary = CoralSecondary,
	secondaryContainer = Color(0xFFFFDBCF),
	onSecondaryContainer = Color(0xFF3A0B00),
	tertiary = Color(0xFF4A635F),
	background = Color(0xFFF4FAF9),
	surface = Color(0xFFF4FAF9),
	surfaceVariant = Color(0xFFDCE5E4),
	onSurface = Color(0xFF141D1C),
	onSurfaceVariant = Color(0xFF3E4948),
	outline = Color(0xFF6E7978)
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFF5DE7E5),
	onPrimary = Color(0xFF003738),
	primaryContainer = Color(0xFF005051),
	onPrimaryContainer = Color(0xFF9EF0EF),
	secondary = Color(0xFFFFB59A),
	secondaryContainer = Color(0xFF8C3314),
	tertiary = Color(0xFFB1CCC7),
	background = Color(0xFF0B1212),
	surface = Color(0xFF0B1212),
	surfaceVariant = Color(0xFF3E4948),
	onSurface = Color(0xFFDCE4E3),
	onSurfaceVariant = Color(0xFFBEC9C8),
	outline = Color(0xFF889392)
)

private val AppShapes = Shapes(
	extraSmall = RoundedCornerShape(8.dp),
	small = RoundedCornerShape(12.dp),
	medium = RoundedCornerShape(16.dp),
	large = RoundedCornerShape(20.dp),
	extraLarge = RoundedCornerShape(28.dp)
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
		shapes = AppShapes,
		content = content
	)
}
