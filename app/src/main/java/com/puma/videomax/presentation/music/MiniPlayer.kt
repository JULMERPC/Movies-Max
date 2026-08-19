package com.puma.videomax.presentation.music

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.service.BackgroundAudioManager
import com.puma.videomax.service.RepeatMode
import com.puma.videomax.util.Formatters

@Composable
fun MiniPlayer(
	onOpenFullPlayer: () -> Unit,
	onClose: () -> Unit,
	modifier: Modifier = Modifier
) {
	val currentIndex by BackgroundAudioManager.currentIndex.collectAsState()
	val queue by BackgroundAudioManager.queue.collectAsState()
	val isPlaying by BackgroundAudioManager.isPlaying.collectAsState()
	val currentPosition by BackgroundAudioManager.currentPosition.collectAsState()
	val duration by BackgroundAudioManager.duration.collectAsState()
	val repeatMode by BackgroundAudioManager.repeatMode.collectAsState()
	val isShuffleEnabled by BackgroundAudioManager.isShuffleEnabled.collectAsState()

	val current = if (currentIndex in queue.indices) queue[currentIndex] else null
	val isVisible = current != null

	val interactionSource = remember { MutableInteractionSource() }
	val isDragging by interactionSource.collectIsDraggedAsState()
	var dragPosition by remember { mutableFloatStateOf(0f) }
	var isDragActive by remember { mutableStateOf(false) }

	val displayPosition = if (isDragActive) dragPosition.toLong() else currentPosition

	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(
			initialOffsetY = { it },
			animationSpec = tween(VideoMaxDimens.animationNormal)
		) + fadeIn(animationSpec = tween(VideoMaxDimens.animationNormal)),
		exit = slideOutVertically(
			targetOffsetY = { it },
			animationSpec = tween(VideoMaxDimens.animationNormal)
		) + fadeOut(animationSpec = tween(VideoMaxDimens.animationNormal)),
		modifier = modifier
	) {
		val item = current ?: return@AnimatedVisibility

		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = onOpenFullPlayer
				),
			shape = RoundedCornerShape(
				topStart = VideoMaxDimens.radiusLg,
				topEnd = VideoMaxDimens.radiusLg
			),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			tonalElevation = VideoMaxDimens.elevationMd,
			shadowElevation = VideoMaxDimens.elevationSm
		) {
			Column(modifier = Modifier.fillMaxWidth()) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp)
						.padding(horizontal = VideoMaxDimens.spacingMd, vertical = VideoMaxDimens.spacingSm),
					verticalAlignment = Alignment.CenterVertically
				) {
					AlbumArt(
						albumId = item.albumId,
						modifier = Modifier
							.size(40.dp)
							.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
					)

					Row(
						modifier = Modifier
							.weight(1f)
							.padding(horizontal = VideoMaxDimens.spacingMd),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = item.displayName,
							style = MaterialTheme.typography.bodyMedium,
							color = VideoMaxTheme.extended.textPrimary,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f)
						)

						if (item.artist.isNotEmpty()) {
							Text(
								text = " · ${item.artist}",
								style = MaterialTheme.typography.bodySmall,
								color = VideoMaxTheme.extended.textTertiary,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
						}
					}

					IconButton(
						onClick = { cycleMode() },
						modifier = Modifier.size(36.dp)
					) {
						val activeColor = MaterialTheme.colorScheme.primary
						val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
						Icon(
							imageVector = when {
								isShuffleEnabled -> Icons.Default.Shuffle
								repeatMode == RepeatMode.ONE -> Icons.Default.RepeatOne
								repeatMode == RepeatMode.ALL -> Icons.Default.Repeat
								else -> Icons.Default.Repeat
							},
							contentDescription = when {
								isShuffleEnabled -> "Aleatorio activado"
								repeatMode == RepeatMode.ONE -> "Repetir: una canción"
								repeatMode == RepeatMode.ALL -> "Repetir: toda la lista"
								else -> "Repetir: desactivado"
							},
							tint = when {
								isShuffleEnabled -> activeColor
								repeatMode != RepeatMode.OFF -> activeColor
								else -> inactiveColor
							},
							modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
						)
					}

					IconButton(
						onClick = onClose,
						modifier = Modifier.size(36.dp)
					) {
						Icon(
							imageVector = Icons.Default.Close,
							contentDescription = "Cerrar",
							tint = VideoMaxTheme.extended.textTertiary,
							modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
						)
					}
				}

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = VideoMaxDimens.spacingMd),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = Formatters.formatDuration(displayPosition),
						style = MaterialTheme.typography.labelSmall,
						color = VideoMaxTheme.extended.textTertiary,
						modifier = Modifier.padding(end = VideoMaxDimens.spacingSm)
					)

					Slider(
						value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
						onValueChange = { fraction ->
							isDragActive = true
							dragPosition = fraction * duration
						},
						onValueChangeFinished = {
							BackgroundAudioManager.seekTo(dragPosition.toLong())
							isDragActive = false
						},
						interactionSource = interactionSource,
						modifier = Modifier.weight(1f),
						colors = SliderDefaults.colors(
							thumbColor = MaterialTheme.colorScheme.primary,
							activeTrackColor = MaterialTheme.colorScheme.primary,
							inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
						)
					)

					Text(
						text = Formatters.formatDuration(duration),
						style = MaterialTheme.typography.labelSmall,
						color = VideoMaxTheme.extended.textTertiary,
						modifier = Modifier.padding(start = VideoMaxDimens.spacingSm)
					)
				}

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(bottom = VideoMaxDimens.spacingSm),
					horizontalArrangement = Arrangement.Center,
					verticalAlignment = Alignment.CenterVertically
				) {
					IconButton(
						onClick = { BackgroundAudioManager.userPrevious() },
						modifier = Modifier.size(44.dp)
					) {
						Icon(
							imageVector = Icons.Default.SkipPrevious,
							contentDescription = "Anterior",
							tint = MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.size(28.dp)
						)
					}

					IconButton(
						onClick = { BackgroundAudioManager.setPlaying(!isPlaying) },
						modifier = Modifier.size(56.dp)
					) {
						Icon(
							imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
							contentDescription = if (isPlaying) "Pausar" else "Reproducir",
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(32.dp)
						)
					}

					IconButton(
						onClick = { BackgroundAudioManager.userNext() },
						modifier = Modifier.size(44.dp)
					) {
						Icon(
							imageVector = Icons.Default.SkipNext,
							contentDescription = "Siguiente",
							tint = MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.size(28.dp)
						)
					}
				}
			}
		}
	}
}

private fun cycleMode() {
	val shuffleOn = BackgroundAudioManager.isShuffleEnabled.value
	val repeat = BackgroundAudioManager.repeatMode.value

	when {
		!shuffleOn && repeat == RepeatMode.OFF -> {
			BackgroundAudioManager.toggleShuffle()
		}
		shuffleOn && repeat == RepeatMode.OFF -> {
			BackgroundAudioManager.toggleShuffle()
			BackgroundAudioManager.cycleRepeatMode()
		}
		!shuffleOn && repeat == RepeatMode.ALL -> {
			BackgroundAudioManager.cycleRepeatMode()
		}
		!shuffleOn && repeat == RepeatMode.ONE -> {
			BackgroundAudioManager.cycleRepeatMode()
		}
	}
}

@Composable
private fun AlbumArt(
	albumId: Long,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current

	if (albumId > 0) {
		val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

		AsyncImage(
			model = ImageRequest.Builder(context)
				.data(albumArtUri)
				.crossfade(true)
				.build(),
			contentDescription = "Portada del álbum",
			contentScale = ContentScale.Crop,
			modifier = modifier
				.background(MaterialTheme.colorScheme.surfaceContainerHighest)
		)
	} else {
		Box(
			modifier = modifier
				.background(
					brush = Brush.linearGradient(
						colors = listOf(
							MaterialTheme.colorScheme.primaryContainer,
							MaterialTheme.colorScheme.secondaryContainer
						)
					)
				),
			contentAlignment = Alignment.Center
		) {
			Icon(
				imageVector = Icons.Default.MusicNote,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onPrimaryContainer,
				modifier = Modifier.size(24.dp)
			)
		}
	}
}
