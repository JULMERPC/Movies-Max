package com.puma.videomax.presentation.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale
import com.puma.videomax.domain.model.Video
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.util.Formatters

@Composable
fun VideoGridItem(
	video: Video,
	onClick: () -> Unit,
	onFavoriteClick: () -> Unit,
	onMenuAction: ((VideoMenuAction) -> Unit)? = null,
	showOnlyThumbnail: Boolean = false,
	modifier: Modifier = Modifier
) {
	var menuExpanded by remember { mutableStateOf(false) }

	if (showOnlyThumbnail) {
		Box(
			modifier = modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
			.clip(RoundedCornerShape(14.dp))
			.background(MaterialTheme.colorScheme.surfaceContainerHigh)
			.clickable(onClick = onClick)
	) {
		VideoThumbnail(
			uri = video.uri,
			cacheKey = "g_${video.id}",
			lightweight = true,
			modifier = Modifier.fillMaxSize()
		)
			if (video.isNew) {
				NewBadge(
					modifier = Modifier
						.align(Alignment.TopStart)
						.padding(8.dp)
				)
			}
			DurationBadge(
				durationMs = video.durationMs,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(8.dp)
			)
			if (video.lastPositionMs > 0 && video.durationMs > 0) {
				ProgressOverlay(
					fraction = video.lastPositionMs.toFloat() / video.durationMs,
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.height(3.dp)
				)
			}
		}
		return
	}

	Column(
		modifier = modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(14.dp))
			.clickable(onClick = onClick)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
				.clip(RoundedCornerShape(14.dp))
				.background(MaterialTheme.colorScheme.surfaceContainerHigh)
		) {
			VideoThumbnail(
				uri = video.uri,
				cacheKey = "c_${video.id}",
				lightweight = true,
				modifier = Modifier.fillMaxSize()
			)
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
						)
					)
			)
			if (video.isNew) {
				NewBadge(
					modifier = Modifier
						.align(Alignment.TopStart)
						.padding(10.dp)
				)
			}
			DurationBadge(
				durationMs = video.durationMs,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(10.dp)
			)
			if (video.lastPositionMs > 0 && video.durationMs > 0) {
				ProgressOverlay(
					fraction = video.lastPositionMs.toFloat() / video.durationMs,
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.height(3.dp)
				)
			}
		}
		Row(
			modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 0.dp, bottom = 2.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = video.displayName,
					style = MaterialTheme.typography.titleSmall,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					color = VideoMaxTheme.extended.textPrimary
				)
				Text(
					text = video.folderName,
					style = MaterialTheme.typography.bodySmall,
					color = VideoMaxTheme.extended.textTertiary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			IconButton(
				onClick = onFavoriteClick,
				modifier = Modifier.size(32.dp)
			) {
				Icon(
					imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
					contentDescription = "Favorite",
					tint = if (video.isFavorite) MaterialTheme.colorScheme.secondary else VideoMaxTheme.extended.textTertiary,
					modifier = Modifier.size(18.dp)
				)
			}
			if (onMenuAction != null) {
				IconButton(
					onClick = { menuExpanded = true },
					modifier = Modifier.size(32.dp)
				) {
					Icon(
						imageVector = Icons.Default.MoreVert,
						contentDescription = "Menu",
						tint = VideoMaxTheme.extended.textTertiary,
						modifier = Modifier.size(18.dp)
					)
				}
				VideoContextMenu(
					expanded = menuExpanded,
					videoId = video.id,
					onDismiss = { menuExpanded = false },
					onAction = onMenuAction
				)
			}
		}
	}
}

@Composable
fun VideoListItem(
	video: Video,
	onClick: () -> Unit,
	onFavoriteClick: () -> Unit,
	onMenuAction: ((VideoMenuAction) -> Unit)? = null,
	modifier: Modifier = Modifier
) {
	var menuExpanded by remember { mutableStateOf(false) }

	Row(
		modifier = modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.width(120.dp)
				.aspectRatio(16f / 9f)
				.clip(RoundedCornerShape(14.dp))
				.background(MaterialTheme.colorScheme.surfaceContainerHigh)
		) {
			VideoThumbnail(
				uri = video.uri,
				cacheKey = "l_${video.id}",
				lightweight = true,
				modifier = Modifier.fillMaxSize()
			)
			Icon(
				imageVector = Icons.Default.PlayArrow,
				contentDescription = null,
				tint = Color.White.copy(alpha = 0.9f),
				modifier = Modifier
					.align(Alignment.Center)
					.size(24.dp)
			)
			DurationBadge(
				durationMs = video.durationMs,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(4.dp)
			)
			if (video.lastPositionMs > 0 && video.durationMs > 0) {
				ProgressOverlay(
					fraction = video.lastPositionMs.toFloat() / video.durationMs,
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.height(2.dp)
				)
			}
		}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = video.displayName,
					style = MaterialTheme.typography.titleSmall,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					color = VideoMaxTheme.extended.textPrimary,
					modifier = Modifier.weight(1f, fill = false)
				)
				if (video.isNew) {
					Spacer(modifier = Modifier.width(6.dp))
					NewBadge()
				}
			}
			Text(
				text = video.folderName,
				style = MaterialTheme.typography.bodySmall,
				color = VideoMaxTheme.extended.textTertiary,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
		IconButton(onClick = onFavoriteClick) {
			Icon(
				imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
				contentDescription = "Favorite",
				tint = if (video.isFavorite) MaterialTheme.colorScheme.secondary else VideoMaxTheme.extended.textTertiary
			)
		}
		if (onMenuAction != null) {
			IconButton(onClick = { menuExpanded = true }) {
				Icon(
					imageVector = Icons.Default.MoreVert,
					contentDescription = "Menu",
					tint = VideoMaxTheme.extended.textTertiary
				)
			}
			VideoContextMenu(
				expanded = menuExpanded,
				videoId = video.id,
				onDismiss = { menuExpanded = false },
				onAction = onMenuAction
			)
		}
	}
}

@Composable
private fun DurationBadge(
	durationMs: Long,
	modifier: Modifier = Modifier
) {
	Text(
		text = Formatters.formatDuration(durationMs),
		style = MaterialTheme.typography.labelSmall,
		color = Color.White,
		modifier = modifier
			.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
			.padding(horizontal = 6.dp, vertical = 2.dp)
	)
}

@Composable
private fun ProgressOverlay(
	fraction: Float,
	modifier: Modifier = Modifier
) {
	LinearProgressIndicator(
		progress = { fraction.coerceIn(0f, 1f) },
		modifier = modifier,
		color = MaterialTheme.colorScheme.primary,
		trackColor = Color.White.copy(alpha = 0.25f)
	)
}

@Composable
fun VideoThumbnail(
	uri: String,
	modifier: Modifier = Modifier,
	cacheKey: String? = null,
	lightweight: Boolean = false
) {
	val context = LocalContext.current
	val model = remember(uri, cacheKey, lightweight) {
		ImageRequest.Builder(context)
			.data(Uri.parse(uri))
			.memoryCacheKey(cacheKey ?: uri)
			.diskCacheKey(cacheKey ?: uri)
			.videoFrameMillis(0)
			.crossfade(200)
			.apply {
				if (lightweight) {
					size(320, 180)
					precision(Precision.INEXACT)
					scale(Scale.FILL)
				}
			}
			.build()
	}
	AsyncImage(
		model = model,
		contentDescription = null,
		contentScale = ContentScale.Crop,
		modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
	)
}

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
	Text(
		text = "NUEVO",
		style = MaterialTheme.typography.labelSmall,
		color = Color.White,
		modifier = modifier
			.background(
				color = MaterialTheme.colorScheme.primary,
				shape = RoundedCornerShape(6.dp)
			)
			.padding(horizontal = 6.dp, vertical = 2.dp)
	)
}

@Composable
fun EmptyState(
	title: String,
	subtitle: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(32.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Icon(
			imageVector = Icons.Default.Movie,
			contentDescription = null,
			modifier = Modifier.size(56.dp),
			tint = VideoMaxTheme.extended.textTertiary
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(
			text = title,
			style = MaterialTheme.typography.titleLarge,
			color = VideoMaxTheme.extended.textPrimary
		)
		Spacer(modifier = Modifier.height(6.dp))
		Text(
			text = subtitle,
			style = MaterialTheme.typography.bodyMedium,
			color = VideoMaxTheme.extended.textSecondary
		)
	}
}

sealed interface VideoMenuAction {
	data class MoveToPrivate(val videoId: Long) : VideoMenuAction
	data class PlayBackground(val videoId: Long) : VideoMenuAction
	data class AddToPlaylist(val videoId: Long) : VideoMenuAction
	data class Rename(val videoId: Long) : VideoMenuAction
	data class Share(val videoId: Long) : VideoMenuAction
	data class Delete(val videoId: Long) : VideoMenuAction
}

@Composable
fun VideoContextMenu(
	expanded: Boolean,
	videoId: Long,
	onDismiss: () -> Unit,
	onAction: (VideoMenuAction) -> Unit
) {
	DropdownMenu(
		expanded = expanded,
		onDismissRequest = onDismiss,
		shape = RoundedCornerShape(16.dp),
		containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
	) {
		DropdownMenuItem(
			text = { Text("Mover a carpeta privada") },
			onClick = { onAction(VideoMenuAction.MoveToPrivate(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
		)
		DropdownMenuItem(
			text = { Text("Reproducir solo audio en fondo") },
			onClick = { onAction(VideoMenuAction.PlayBackground(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null) }
		)
		DropdownMenuItem(
			text = { Text("Añadir a lista") },
			onClick = { onAction(VideoMenuAction.AddToPlaylist(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
		)
		DropdownMenuItem(
			text = { Text("Renombrar") },
			onClick = { onAction(VideoMenuAction.Rename(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
		)
		DropdownMenuItem(
			text = { Text("Compartir") },
			onClick = { onAction(VideoMenuAction.Share(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
		)
		DropdownMenuItem(
			text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
			onClick = { onAction(VideoMenuAction.Delete(videoId)); onDismiss() },
			leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
		)
	}
}

@Composable
fun ShimmerBox(
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "shimmer")
	val alpha by transition.animateFloat(
		initialValue = 0.3f,
		targetValue = 0.6f,
		animationSpec = infiniteRepeatable(
			animation = tween(800, easing = LinearEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "shimmerAlpha"
	)
	Box(
		modifier = modifier
			.background(
				MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
				RoundedCornerShape(8.dp)
			)
	)
}
