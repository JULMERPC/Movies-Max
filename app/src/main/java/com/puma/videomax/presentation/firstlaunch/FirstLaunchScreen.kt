package com.puma.videomax.presentation.firstlaunch

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import kotlinx.coroutines.launch

@Composable
fun FirstLaunchScreen(
	selectedAccentColor: Long,
	onColorSelected: (Long) -> Unit,
	onAccept: () -> Unit
) {
	val pagerState = rememberPagerState(pageCount = { 3 })
	val scope = rememberCoroutineScope()
	val accentColor = if (selectedAccentColor != 0L) Color(selectedAccentColor.toInt())
		else MaterialTheme.colorScheme.primary

	Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
				.background(
					Brush.verticalGradient(
						colors = listOf(
							accentColor.copy(alpha = 0.18f),
							Color.Transparent
						)
					)
				)
		)

		Column(modifier = Modifier.fillMaxSize()) {
			TextButton(
				onClick = onAccept,
				modifier = Modifier
					.align(Alignment.End)
					.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm)
			) {
				Text("Omitir", color = Color.Gray)
			}

			HorizontalPager(
				state = pagerState,
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
			) { page ->
				when (page) {
					0 -> WelcomePage(accentColor)
					1 -> FeaturesPage(accentColor, onColorSelected, selectedAccentColor)
					2 -> TermsPage(accentColor)
				}
			}

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(VideoMaxDimens.spacingLg),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically
			) {
				repeat(3) { index ->
					val dotColor by animateColorAsState(
						targetValue = if (pagerState.currentPage == index) accentColor
						else Color(0xFFD0D0D0),
						label = "dot"
					)
					Box(
						modifier = Modifier
							.padding(horizontal = 4.dp)
							.size(if (pagerState.currentPage == index) 10.dp else 8.dp)
							.clip(CircleShape)
							.background(dotColor)
					)
				}
			}

			Button(
				onClick = {
					if (pagerState.currentPage < 2) {
						scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
					} else {
						onAccept()
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = VideoMaxDimens.spacingLg)
					.padding(bottom = VideoMaxDimens.spacingXxl)
					.height(56.dp),
				shape = RoundedCornerShape(VideoMaxDimens.radiusMd),
				colors = ButtonDefaults.buttonColors(containerColor = accentColor)
			) {
				Text(
					if (pagerState.currentPage < 2) "Siguiente" else "Aceptar y continuar",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					color = Color.White
				)
			}
		}
	}
}

@Composable
private fun WelcomePage(accentColor: Color) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(VideoMaxDimens.spacingXxl),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = Icons.Default.PlayCircle,
			contentDescription = null,
			modifier = Modifier.size(100.dp),
			tint = accentColor
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))
		Text(
			text = "Bienvenido a Jualix",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.Bold,
			color = Color(0xFF1A1A1A),
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))
		Text(
			text = "Tu reproductor de video y música favorito.\nDescubrí una nueva forma de disfrutar tu contenido.",
			style = MaterialTheme.typography.bodyLarge,
			color = Color(0xFF666666),
			textAlign = TextAlign.Center
		)
	}
}

@Composable
private fun FeaturesPage(
	accentColor: Color,
	onColorSelected: (Long) -> Unit,
	selectedAccentColor: Long
) {
	var showColorPicker by remember { mutableStateOf(false) }

	val presetColors = listOf(
		0xFF006B5EL,
		0xFFE91E63L,
		0xFF2196F3L,
		0xFFFF9800L,
		0xFF9C27B0L,
		0xFF4CAF50L
	)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(VideoMaxDimens.spacingXxl),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = "¿Qué podés hacer?",
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = Color(0xFF1A1A1A),
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))
		FeatureItem(
			icon = Icons.Default.VideoLibrary,
			title = "Biblioteca de videos",
			description = "Organizá y accedé a todos tus videos",
			accentColor = accentColor
		)
		FeatureItem(
			icon = Icons.Default.Headphones,
			title = "Música",
			description = "Escuchá tu música favorita con un reproductor completo",
			accentColor = accentColor
		)
		FeatureItem(
			icon = Icons.Default.PlaylistPlay,
			title = "Listas de reproducción",
			description = "Creá y administrá tus listas personalizadas",
			accentColor = accentColor
		)
		FeatureItem(
			icon = Icons.Default.Lock,
			title = "Carpeta privada",
			description = "Protegé tus videos más personales con un PIN",
			accentColor = accentColor
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXl))

		Text(
			text = "Elegí tu color favorito",
			style = MaterialTheme.typography.bodyLarge,
			color = Color(0xFF1A1A1A),
			fontWeight = FontWeight.Medium
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
		Row(horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm)) {
			presetColors.forEach { color ->
				val isSelected = selectedAccentColor == color
				Box(
					modifier = Modifier
						.size(40.dp)
						.clip(CircleShape)
						.background(Color(color.toInt()))
						.then(
							if (isSelected) Modifier.border(3.dp, Color(0xFF1A1A1A), CircleShape)
							else Modifier
						)
						.clickable {
							onColorSelected(color)
						}
				)
			}
		}
	}
}

@Composable
private fun FeatureItem(
	icon: ImageVector,
	title: String,
	description: String,
	accentColor: Color
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = VideoMaxDimens.spacingSm),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			modifier = Modifier.size(36.dp),
			tint = accentColor
		)
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingLg))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = Color(0xFF1A1A1A)
			)
			Text(
				text = description,
				style = MaterialTheme.typography.bodyMedium,
				color = Color(0xFF888888)
			)
		}
	}
}

@Composable
private fun TermsPage(accentColor: Color) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(VideoMaxDimens.spacingXxl),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = Icons.Default.Security,
			contentDescription = null,
			modifier = Modifier.size(80.dp),
			tint = accentColor
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))
		Text(
			text = "Términos y Privacidad",
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			color = Color(0xFF1A1A1A),
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
		Text(
			text = "Para usar Jualix, necesitás aceptar nuestros Términos de Servicio y Política de Privacidad.",
			style = MaterialTheme.typography.bodyLarge,
			color = Color(0xFF666666),
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXl))
		Text(
			text = "Términos de Servicio",
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium,
			color = accentColor
		)
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
		Text(
			text = "Política de Privacidad",
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium,
			color = accentColor
		)
	}
}
