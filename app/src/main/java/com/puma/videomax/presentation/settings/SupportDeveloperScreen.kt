package com.puma.videomax.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.puma.videomax.domain.monetization.MonetizationRepository.Companion.REWARDED_REQUIRED
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeveloperScreen(
	onBack: () -> Unit,
	viewModel: SupportDeveloperViewModel = hiltViewModel()
) {
	val adState by viewModel.adState.collectAsStateWithLifecycle()
	val showThankYou by viewModel.showThankYou.collectAsStateWithLifecycle()
	val monetization by viewModel.monetizationState.collectAsStateWithLifecycle()
	val passRemainingMs by viewModel.passRemainingMs.collectAsStateWithLifecycle()
	val activity = LocalActivity.current

	val passActive = passRemainingMs > 0L

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenColor())
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
					.verticalScroll(rememberScrollState())
					.padding(horizontal = VideoMaxDimens.spacingXxl)
					.padding(bottom = VideoMaxDimens.spacingXxxl),
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
					text = "Jualix se desarrolla de forma independiente.\nSi disfrutas de la aplicación, puedes apoyar su desarrollo\nviendo voluntariamente dos breves anuncios.",
					style = MaterialTheme.typography.bodyLarge,
					color = VideoMaxTheme.extended.textTertiary,
					textAlign = TextAlign.Center
				)

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))

				when {
					monetization.isPremium -> {
						PremiumActiveCard()
					}

					passActive -> {
						AdFreePassCard(remainingText = formatPassRemaining(passRemainingMs))
					}

					else -> {
						RewardProgressCard(progress = monetization.rewardedProgress)

						Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))

						AnimatedVisibility(
							visible = showThankYou,
							enter = fadeIn(),
							exit = fadeOut()
						) {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								if (passActive) {
									Text(
										text = "¡Pase de 24 horas activado!",
										style = MaterialTheme.typography.titleMedium,
										fontWeight = FontWeight.Bold,
										color = MaterialTheme.colorScheme.primary,
										textAlign = TextAlign.Center,
									)
								} else {
									Text(
										text = "¡Gracias por apoyar Jualix!",
										style = MaterialTheme.typography.titleMedium,
										fontWeight = FontWeight.Bold,
										color = MaterialTheme.colorScheme.primary,
										textAlign = TextAlign.Center,
									)
									Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
									Text(
										text = "Mirá 1 video más y desactivá\ntodos los anuncios por 24 horas.",
										style = MaterialTheme.typography.bodyMedium,
										color = VideoMaxTheme.extended.textTertiary,
										textAlign = TextAlign.Center,
										modifier = Modifier.padding(bottom = VideoMaxDimens.spacingLg)
									)
								}
							}
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
										text = "Ver video ${monetization.rewardedProgress + 1} de $REWARDED_REQUIRED",
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
	}
}

@Composable
private fun RewardProgressCard(progress: Int) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
		),
		shape = MaterialTheme.shapes.large
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(VideoMaxDimens.spacingXl),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = "Mirá 2 videos breves para desactivar\nTODOS los anuncios por 24 horas",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = VideoMaxTheme.extended.textPrimary,
				textAlign = TextAlign.Center
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))

			Row(
				horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
				verticalAlignment = Alignment.CenterVertically
			) {
				repeat(REWARDED_REQUIRED) { index ->
					val achieved = index < progress
					Box(
						modifier = Modifier
							.size(40.dp)
							.background(
								color = if (achieved) MaterialTheme.colorScheme.primary
								else MaterialTheme.colorScheme.surfaceContainerHighest,
								shape = CircleShape
							),
						contentAlignment = Alignment.Center
					) {
						if (achieved) {
							Icon(
								imageVector = Icons.Default.Check,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.onPrimary,
								modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
							)
						} else {
							Text(
								text = "${index + 1}",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.Bold,
								color = VideoMaxTheme.extended.textTertiary
							)
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))

			LinearProgressIndicator(
				progress = { progress / REWARDED_REQUIRED.toFloat() },
				modifier = Modifier.fillMaxWidth(),
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))

			Text(
				text = "$progress de $REWARDED_REQUIRED videos",
				style = MaterialTheme.typography.bodyMedium,
				color = VideoMaxTheme.extended.textTertiary
			)
		}
	}
}

@Composable
private fun AdFreePassCard(remainingText: String) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		),
		shape = MaterialTheme.shapes.large
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(VideoMaxDimens.spacingXl),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(
				imageVector = Icons.Default.Timer,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onPrimaryContainer,
				modifier = Modifier.size(48.dp)
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))

			Text(
				text = "Navegación sin anuncios",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				textAlign = TextAlign.Center
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))

			Text(
				text = "Te quedan $remainingText",
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				textAlign = TextAlign.Center
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))

			Text(
				text = "Todos los anuncios están desactivados.\nVolvé cuando el pase termine para renovarlo.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
				textAlign = TextAlign.Center
			)
		}
	}
}

@Composable
private fun PremiumActiveCard() {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		),
		shape = MaterialTheme.shapes.large
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(VideoMaxDimens.spacingXl),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(
				imageVector = Icons.Default.Check,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onPrimaryContainer,
				modifier = Modifier.size(48.dp)
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))

			Text(
				text = "Jualix Premium activo",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				textAlign = TextAlign.Center
			)

			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))

			Text(
				text = "Disfrutás la app sin anuncios, para siempre.\n¡Gracias por tu apoyo!",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
				textAlign = TextAlign.Center
			)
		}
	}
}
