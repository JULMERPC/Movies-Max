package com.example.videomax.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videomax.domain.model.ThemeMode

@Immutable
data class ExtendedColors(
	val surfaceGlass: Color,
	val surfaceGlassHover: Color,
	val borderSubtle: Color,
	val textPrimary: Color,
	val textSecondary: Color,
	val textTertiary: Color,
	val scrim: Color,
	val overlay: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
	ExtendedColors(
		surfaceGlass = Color.Unspecified,
		surfaceGlassHover = Color.Unspecified,
		borderSubtle = Color.Unspecified,
		textPrimary = Color.Unspecified,
		textSecondary = Color.Unspecified,
		textTertiary = Color.Unspecified,
		scrim = Color.Unspecified,
		overlay = Color.Unspecified
	)
}

private val LightExtended = ExtendedColors(
	surfaceGlass = Color(0x0D000000),
	surfaceGlassHover = Color(0x14000000),
	borderSubtle = Color(0x14000000),
	textPrimary = Color(0xE6000000),
	textSecondary = Color(0x99000000),
	textTertiary = Color(0x66000000),
	scrim = Color(0x80000000),
	overlay = Color(0x0D000000)
)

private val DarkExtended = ExtendedColors(
	surfaceGlass = Color(0x1AFFFFFF),
	surfaceGlassHover = Color(0x26FFFFFF),
	borderSubtle = Color(0x14FFFFFF),
	textPrimary = Color(0xF5FFFFFF),
	textSecondary = Color(0xB3FFFFFF),
	textTertiary = Color(0x80FFFFFF),
	scrim = Color(0x99000000),
	overlay = Color(0x1A000000)
)

private val AmoledExtended = ExtendedColors(
	surfaceGlass = Color(0x0DFFFFFF),
	surfaceGlassHover = Color(0x1AFFFFFF),
	borderSubtle = Color(0x0FFFFFFF),
	textPrimary = Color(0xF5FFFFFF),
	textSecondary = Color(0xB3FFFFFF),
	textTertiary = Color(0x80FFFFFF),
	scrim = Color(0xCC000000),
	overlay = Color(0x0D000000)
)

private val LightColors = lightColorScheme(
	primary = Color(0xFF006B5E),
	onPrimary = Color.White,
	primaryContainer = Color(0xFF7AF8E3),
	onPrimaryContainer = Color(0xFF00201B),
	secondary = Color(0xFF8B5000),
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFFFDCBE),
	onSecondaryContainer = Color(0xFF2D1600),
	tertiary = Color(0xFF4A6178),
	tertiaryContainer = Color(0xFFD2E5FF),
	background = Color(0xFFF5FBF9),
	onBackground = Color(0xFF171D1B),
	surface = Color(0xFFF5FBF9),
	onSurface = Color(0xFF171D1B),
	surfaceVariant = Color(0xFFDAE5E1),
	onSurfaceVariant = Color(0xFF3F4945),
	surfaceContainerLow = Color(0xFFEFF5F3),
	surfaceContainer = Color(0xFFE9EFED),
	surfaceContainerHigh = Color(0xFFE3EAE8),
	surfaceContainerHighest = Color(0xFFDDE4E2),
	outline = Color(0xFF6F7975),
	outlineVariant = Color(0xFFBEC9C5)
)

private val DarkColors = darkColorScheme(
	primary = Color(0xFF5DDBCA),
	onPrimary = Color(0xFF003730),
	primaryContainer = Color(0xFF005047),
	onPrimaryContainer = Color(0xFF7AF8E3),
	secondary = Color(0xFFFFB86C),
	onSecondary = Color(0xFF4A2800),
	secondaryContainer = Color(0xFF6A3C00),
	onSecondaryContainer = Color(0xFFFFDCBE),
	tertiary = Color(0xFFB2C9E2),
	tertiaryContainer = Color(0xFF334960),
	background = Color(0xFF0E1514),
	onBackground = Color(0xFFDDE4E2),
	surface = Color(0xFF0E1514),
	onSurface = Color(0xFFDDE4E2),
	surfaceVariant = Color(0xFF3F4945),
	onSurfaceVariant = Color(0xFFBEC9C5),
	surfaceContainerLow = Color(0xFF121A18),
	surfaceContainer = Color(0xFF171F1D),
	surfaceContainerHigh = Color(0xFF1C2422),
	surfaceContainerHighest = Color(0xFF212927),
	outline = Color(0xFF89938F),
	outlineVariant = Color(0xFF3F4945)
)

private val AmoledColors = darkColorScheme(
	primary = Color(0xFF5DDBCA),
	onPrimary = Color(0xFF003730),
	primaryContainer = Color(0xFF005047),
	onPrimaryContainer = Color(0xFF7AF8E3),
	secondary = Color(0xFFFFB86C),
	onSecondary = Color(0xFF4A2800),
	secondaryContainer = Color(0xFF6A3C00),
	onSecondaryContainer = Color(0xFFFFDCBE),
	tertiary = Color(0xFFB2C9E2),
	tertiaryContainer = Color(0xFF334960),
	background = Color.Black,
	onBackground = Color(0xFFDDE4E2),
	surface = Color.Black,
	onSurface = Color(0xFFDDE4E2),
	surfaceVariant = Color(0xFF1A1A1A),
	onSurfaceVariant = Color(0xFFBEC9C5),
	surfaceContainerLow = Color(0xFF0A0A0A),
	surfaceContainer = Color(0xFF111111),
	surfaceContainerHigh = Color(0xFF1A1A1A),
	surfaceContainerHighest = Color(0xFF222222),
	outline = Color(0xFF89938F),
	outlineVariant = Color(0xFF2A2A2A)
)

private val AppShapes = Shapes(
	extraSmall = RoundedCornerShape(6.dp),
	small = RoundedCornerShape(8.dp),
	medium = RoundedCornerShape(14.dp),
	large = RoundedCornerShape(20.dp),
	extraLarge = RoundedCornerShape(28.dp)
)

private fun Color.alphaBlendWithWhite(alphaFraction: Float): Color =
	Color(this.red, this.green, this.blue, alphaFraction).compositeOver(Color.White)

@Composable
fun VideoPlayerProTheme(
	themeMode: ThemeMode = ThemeMode.SYSTEM,
	content: @Composable () -> Unit
) {
	val darkTheme = when (themeMode) {
		ThemeMode.LIGHT -> false
		ThemeMode.DARK, ThemeMode.AMOLED -> true
		ThemeMode.SYSTEM -> isSystemInDarkTheme()
	}
	val isAmoled = themeMode == ThemeMode.AMOLED

	val context = LocalContext.current
	val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

	val colors: ColorScheme = when {
		dynamicColorAvailable && darkTheme -> dynamicDarkColorScheme(context)
		dynamicColorAvailable && !darkTheme -> dynamicLightColorScheme(context)
		isAmoled -> AmoledColors
		darkTheme -> DarkColors
		else -> LightColors
	}

	val extendedColors = when {
		isAmoled -> AmoledExtended
		darkTheme -> DarkExtended
		else -> LightExtended
	}

	val scaffoldColor = colors.primary.alphaBlendWithWhite(60f / 255f)

	CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
		MaterialTheme(
			colorScheme = colors.copy(
				surface = scaffoldColor,
				background = scaffoldColor
			),
			typography = Typography,
			shapes = AppShapes,
			content = content
		)
	}
}

object VideoMaxTheme {
	val extended: ExtendedColors
		@Composable get() = LocalExtendedColors.current
}
