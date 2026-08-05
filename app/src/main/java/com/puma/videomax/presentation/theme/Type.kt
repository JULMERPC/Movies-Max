package com.puma.videomax.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.puma.videomax.R

private val LexendDeca = FontFamily(
	Font(R.font.lexend_deca, FontWeight.Normal),
	Font(R.font.lexend_deca, FontWeight.Medium),
	Font(R.font.lexend_deca, FontWeight.SemiBold),
	Font(R.font.lexend_deca, FontWeight.Bold)
)

val Typography = Typography(
	headlineLarge = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Bold,
		fontSize = 28.sp,
		lineHeight = 34.sp,
		letterSpacing = (-0.5).sp
	),
	headlineMedium = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.SemiBold,
		fontSize = 22.sp,
		lineHeight = 28.sp,
		letterSpacing = (-0.3).sp
	),
	headlineSmall = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.SemiBold,
		fontSize = 18.sp,
		lineHeight = 24.sp,
		letterSpacing = (-0.2).sp
	),
	titleLarge = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.SemiBold,
		fontSize = 18.sp,
		lineHeight = 24.sp,
		letterSpacing = (-0.2).sp
	),
	titleMedium = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Medium,
		fontSize = 15.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.1.sp
	),
	titleSmall = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Medium,
		fontSize = 13.sp,
		lineHeight = 18.sp,
		letterSpacing = 0.1.sp
	),
	bodyLarge = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Normal,
		fontSize = 15.sp,
		lineHeight = 22.sp,
		letterSpacing = 0.1.sp
	),
	bodyMedium = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Normal,
		fontSize = 13.sp,
		lineHeight = 18.sp,
		letterSpacing = 0.2.sp
	),
	bodySmall = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Normal,
		fontSize = 11.sp,
		lineHeight = 15.sp,
		letterSpacing = 0.3.sp
	),
	labelLarge = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Medium,
		fontSize = 13.sp,
		lineHeight = 18.sp,
		letterSpacing = 0.1.sp
	),
	labelMedium = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Medium,
		fontSize = 11.sp,
		lineHeight = 15.sp,
		letterSpacing = 0.5.sp
	),
	labelSmall = TextStyle(
		fontFamily = LexendDeca,
		fontWeight = FontWeight.Medium,
		fontSize = 9.sp,
		lineHeight = 13.sp,
		letterSpacing = 0.5.sp
	)
)
