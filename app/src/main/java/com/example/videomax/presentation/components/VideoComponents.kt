package com.example.videomax.presentation.components

import android.net.Uri
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
import com.example.videomax.domain.model.Video
import com.example.videomax.util.Formatters

/**
 * Grid cell with large thumbnail plus title / folder / size description.
 */
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
				.clip(RoundedCornerShape(10.dp))
				.background(MaterialTheme.colorScheme.surfaceVariant)
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
						.padding(6.dp)
				)
			}
			Text(
				text = Formatters.formatDuration(video.durationMs),
				style = MaterialTheme.typography.labelSmall,
				color = Color.White,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(6.dp)
					.background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
					.padding(horizontal = 5.dp, vertical = 2.dp)
			)
			if (video.lastPositionMs > 0 && video.durationMs > 0) {
				LinearProgressIndicator(
					progress = { (video.lastPositionMs.toFloat() / video.durationMs).coerceIn(0f, 1f) },
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.height(2.dp),
					color = MaterialTheme.colorScheme.primary,
					trackColor = Color.Transparent
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
				.background(MaterialTheme.colorScheme.surfaceVariant)
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
							listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
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
			Text(
				text = Formatters.formatDuration(video.durationMs),
				style = MaterialTheme.typography.labelMedium,
				color = Color.White,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(10.dp)
					.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
					.padding(horizontal = 7.dp, vertical = 3.dp)
			)
			Row(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(2.dp),
				horizontalArrangement = Arrangement.End
			) {
				IconButton(onClick = onFavoriteClick) {
					Icon(
						imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
						contentDescription = "Favorite",
						tint = if (video.isFavorite) MaterialTheme.colorScheme.secondary else Color.White
					)
				}
				if (onMenuAction != null) {
					IconButton(onClick = { menuExpanded = true }) {
						Icon(
							imageVector = Icons.Default.MoreVert,
							contentDescription = "Menu",
							tint = Color.White
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
			if (video.lastPositionMs > 0 && video.durationMs > 0) {
				LinearProgressIndicator(
					progress = { (video.lastPositionMs.toFloat() / video.durationMs).coerceIn(0f, 1f) },
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.height(3.dp),
					color = MaterialTheme.colorScheme.primary,
					trackColor = Color.White.copy(alpha = 0.25f)
				)
			}
		}
		Column(modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp, bottom = 2.dp)) {
			Text(
				text = video.displayName,
				style = MaterialTheme.typography.titleSmall,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = video.folderName,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = "${Formatters.formatFileSize(video.sizeBytes)} · ${video.resolutionLabel}",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
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
			.padding(horizontal = 16.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.width(120.dp)
				.aspectRatio(16f / 9f)
				.clip(RoundedCornerShape(8.dp))
				.background(MaterialTheme.colorScheme.surfaceVariant)
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
					.size(28.dp)
			)
		}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = video.displayName,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f, fill = false)
				)
				if (video.isNew) {
					Spacer(modifier = Modifier.width(6.dp))
					NewBadge()
				}
			}
			Text(
				text = "${Formatters.formatDuration(video.durationMs)} · ${video.resolutionLabel}",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		IconButton(onClick = onFavoriteClick) {
			Icon(
				imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
				contentDescription = "Favorite",
				tint = if (video.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (onMenuAction != null) {
			IconButton(onClick = { menuExpanded = true }) {
				Icon(
					imageVector = Icons.Default.MoreVert,
					contentDescription = "Menu",
					tint = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * @param lightweight smaller decode target + first frame — for grids/lists to avoid UI stalls.
 */
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
			// Frame 0 is much cheaper than seeking to 1s on every cell.
			.videoFrameMillis(0)
			.crossfade(false)
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
		modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
	)
}

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
	Text(
		text = "NEW",
		style = MaterialTheme.typography.labelSmall,
		color = Color.White,
		modifier = modifier
			.background(
				color = MaterialTheme.colorScheme.primary,
				shape = RoundedCornerShape(4.dp)
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
			modifier = Modifier.size(64.dp),
			tint = MaterialTheme.colorScheme.primary
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(text = title, style = MaterialTheme.typography.headlineMedium)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = subtitle,
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
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
		onDismissRequest = onDismiss
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
