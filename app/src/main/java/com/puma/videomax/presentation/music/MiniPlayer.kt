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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

	val current = if (currentIndex in queue.indices) queue[currentIndex] else null
	val isVisible = current != null

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
				.clickable(onClick = onOpenFullPlayer),
			shape = RoundedCornerShape(
				topStart = VideoMaxDimens.radiusLg,
				topEnd = VideoMaxDimens.radiusLg
			),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			tonalElevation = VideoMaxDimens.elevationMd,
			shadowElevation = VideoMaxDimens.elevationSm
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(72.dp)
					.padding(horizontal = VideoMaxDimens.spacingMd, vertical = VideoMaxDimens.spacingSm),
				verticalAlignment = Alignment.CenterVertically
			) {
				AlbumArt(
					albumId = item.albumId,
					modifier = Modifier
						.size(48.dp)
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
					onClick = {
						BackgroundAudioManager.setPlaying(!isPlaying)
					},
					modifier = Modifier.size(40.dp)
				) {
					Icon(
						imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
						contentDescription = if (isPlaying) "Pausar" else "Reproducir",
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(VideoMaxDimens.iconSizeLg)
					)
				}

				IconButton(
					onClick = onClose,
					modifier = Modifier.size(40.dp)
				) {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = "Cerrar",
						tint = VideoMaxTheme.extended.textTertiary,
						modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
					)
				}
			}

			if (duration > 0) {
				val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
				LinearProgressIndicator(
					progress = { progress },
					modifier = Modifier
						.fillMaxWidth()
						.height(2.dp),
					color = MaterialTheme.colorScheme.primary,
					trackColor = Color.Transparent
				)
			}
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
