package com.puma.videomax.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeveloperScreen(
	onBack: () -> Unit,
	viewModel: SupportDeveloperViewModel = hiltViewModel()
) {
	val adState by viewModel.adState.collectAsStateWithLifecycle()
	val showThankYou by viewModel.showThankYou.collectAsStateWithLifecycle()
	val activity = LocalActivity.current

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenGradient())
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			TopAppBar(
				title = {
					Text(
						"Apoya al desarrollador",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold,
						color = VideoMaxTheme.extended.textPrimary
					)
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Volver",
							tint = VideoMaxTheme.extended.textPrimary
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent
				)
			)

			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = VideoMaxDimens.spacingXxl),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center
			) {
				Icon(
					imageVector = Icons.Default.Favorite,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(64.dp)
				)

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))

				Text(
					text = "Apoya el desarrollo de Jualix",
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
					color = VideoMaxTheme.extended.textPrimary,
					textAlign = TextAlign.Center
				)

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))

				Text(
					text = "Jualix se desarrolla de forma independiente.\nSi disfrutas de la aplicación, puedes apoyar su desarrollo\nviendo voluntariamente un breve anuncio.",
					style = MaterialTheme.typography.bodyLarge,
					color = VideoMaxTheme.extended.textTertiary,
					textAlign = TextAlign.Center
				)

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxxxl))

				AnimatedVisibility(
					visible = showThankYou,
					enter = fadeIn(),
					exit = fadeOut()
				) {
					Text(
						text = "¡Gracias por apoyar Jualix! ❤️",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary,
						textAlign = TextAlign.Center,
						modifier = Modifier.padding(bottom = VideoMaxDimens.spacingLg)
					)
				}

				when (adState) {
					RewardedAdState.LOADING -> {
						CircularProgressIndicator(
							color = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(32.dp)
						)
						Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))
						Text(
							text = "Preparando anuncio...",
							style = MaterialTheme.typography.bodyMedium,
							color = VideoMaxTheme.extended.textTertiary
						)
					}

					RewardedAdState.READY -> {
						Button(
							onClick = { activity?.let { viewModel.showAd(it) } },
							colors = ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.primary
							),
							modifier = Modifier.fillMaxWidth(0.7f)
						) {
							Text(
								text = "Ver anuncio",
								style = MaterialTheme.typography.titleMedium
							)
						}
					}

					RewardedAdState.SHOWING -> {
						CircularProgressIndicator(
							color = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(32.dp)
						)
					}

					RewardedAdState.COMPLETED -> {
						// Thank you message is shown above via AnimatedVisibility
					}

					RewardedAdState.ERROR, RewardedAdState.UNAVAILABLE -> {
						Text(
							text = "Anuncio no disponible.\nPodés intentar más tarde.",
							style = MaterialTheme.typography.bodyMedium,
							color = VideoMaxTheme.extended.textTertiary,
							textAlign = TextAlign.Center
						)
						Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
						Button(
							onClick = { viewModel.loadAd() },
							colors = ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
							),
							modifier = Modifier.fillMaxWidth(0.7f)
						) {
							Text(
								text = "Reintentar",
								style = MaterialTheme.typography.titleMedium
							)
						}
					}
				}
			}
		}
	}
}
