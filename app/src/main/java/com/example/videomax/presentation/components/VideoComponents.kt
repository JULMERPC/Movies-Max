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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.ui.platform.LocalContext
import com.example.videomax.domain.model.Video
import com.example.videomax.util.Formatters

@Composable
fun VideoGridItem(
	video: Video,
	onClick: () -> Unit,
	onFavoriteClick: () -> Unit,
	onLongClick: (() -> Unit)? = null,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(18.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
	) {
		Column {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(16f / 9f)
			) {
				VideoThumbnail(
					uri = video.uri,
					modifier = Modifier.fillMaxSize()
				)
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.verticalGradient(
								listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
							)
						)
				)
				Text(
					text = Formatters.formatDuration(video.durationMs),
					style = MaterialTheme.typography.labelLarge,
					color = Color.White,
					modifier = Modifier
						.align(Alignment.BottomEnd)
						.padding(8.dp)
						.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
						.padding(horizontal = 6.dp, vertical = 2.dp)
				)
				IconButton(
					onClick = onFavoriteClick,
					modifier = Modifier.align(Alignment.TopEnd)
				) {
					Icon(
						imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
						contentDescription = "Favorite",
						tint = if (video.isFavorite) MaterialTheme.colorScheme.secondary else Color.White
					)
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
			Column(modifier = Modifier.padding(12.dp)) {
				Text(
					text = video.displayName,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = "${video.folderName} · ${Formatters.formatFileSize(video.sizeBytes)}",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
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
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.width(128.dp)
				.aspectRatio(16f / 9f)
				.clip(RoundedCornerShape(12.dp))
		) {
			VideoThumbnail(uri = video.uri, modifier = Modifier.fillMaxSize())
			Icon(
				imageVector = Icons.Default.PlayArrow,
				contentDescription = null,
				tint = Color.White,
				modifier = Modifier
					.align(Alignment.Center)
					.size(28.dp)
			)
		}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = video.displayName,
				style = MaterialTheme.typography.titleMedium,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
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
	}
}

@Composable
fun VideoThumbnail(
	uri: String,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	AsyncImage(
		model = ImageRequest.Builder(context)
			.data(Uri.parse(uri))
			.decoderFactory(VideoFrameDecoder.Factory())
			.videoFrameMillis(1_000)
			.crossfade(true)
			.build(),
		contentDescription = null,
		contentScale = ContentScale.Crop,
		modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
		error = null,
		placeholder = null
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
