package com.puma.videomax.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Rational
import android.view.PixelCopy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.util.Formatters
import kotlinx.coroutines.flow.StateFlow

internal fun enterPip(activity: Activity, width: Int, height: Int) {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
	val w = width.coerceAtLeast(1)
	val h = height.coerceAtLeast(1)
	val params = PictureInPictureParams.Builder()
		.setAspectRatio(Rational(w, h))
		.build()
	activity.enterPictureInPictureMode(params)
}

internal fun captureFrame(
	activity: Activity,
	playerView: androidx.media3.ui.PlayerView?,
	onResult: (Bitmap?) -> Unit
) {
	val surfaceView = playerView?.videoSurfaceView as? android.view.SurfaceView ?: run {
		onResult(null)
		return
	}
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
		onResult(null)
		return
	}
	val bitmap = Bitmap.createBitmap(
		surfaceView.width.coerceAtLeast(1),
		surfaceView.height.coerceAtLeast(1),
		Bitmap.Config.ARGB_8888
	)
	PixelCopy.request(
		surfaceView,
		bitmap,
		{ copyResult ->
			if (copyResult == PixelCopy.SUCCESS) onResult(bitmap) else onResult(null)
		},
		Handler(Looper.getMainLooper())
	)
}

internal fun saveBitmapToGallery(activity: Activity, bitmap: Bitmap): Boolean {
	val filename = "videomax_${System.currentTimeMillis()}.jpg"
	val contentValues = ContentValues().apply {
		put(MediaStore.Images.Media.DISPLAY_NAME, filename)
		put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/videomax")
			put(MediaStore.Images.Media.IS_PENDING, 1)
		}
	}
	val resolver = activity.contentResolver
	val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
		?: return false
	return try {
		resolver.openOutputStream(uri)?.use { os ->
			bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			val update = ContentValues().apply {
				put(MediaStore.Images.Media.IS_PENDING, 0)
			}
			resolver.update(uri, update, null, null)
		}
		true
	} catch (_: Exception) {
		resolver.delete(uri, null, null)
		false
	}
}

@Composable
internal fun PlayerGestureHint(
	hint: String?,
	icon: ImageVector? = null,
	modifier: Modifier = Modifier
) {
	AnimatedVisibility(
		visible = hint != null,
		enter = fadeIn(tween(VideoMaxDimens.animationFast)),
		exit = fadeOut(tween(VideoMaxDimens.animationFast)),
		modifier = modifier
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.background(
					Color.Black.copy(alpha = VideoMaxDimens.alphaOverlay),
					RoundedCornerShape(VideoMaxDimens.radiusLg)
				)
				.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingMd)
		) {
			if (icon != null) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = Color.White,
					modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
				)
			}
			Text(
				text = hint.orEmpty(),
				color = Color.White,
				style = MaterialTheme.typography.titleMedium,
				modifier = if (icon != null) Modifier.padding(start = VideoMaxDimens.spacingSm) else Modifier
			)
		}
	}
}

@Composable
internal fun PlayerTopBar(
	fileName: String,
	onBack: () -> Unit,
	onOpenQueue: () -> Unit
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier
			.fillMaxWidth()
			.background(
				Brush.verticalGradient(
					listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
				)
			)
			.statusBarsPadding()
			.padding(horizontal = VideoMaxDimens.spacingXs, vertical = VideoMaxDimens.spacingSm)
	) {
		RippleIconButton(onClick = onBack) {
			Icon(
				Icons.AutoMirrored.Filled.ArrowBack,
				"Volver",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeLg)
			)
		}
		RippleIconButton(onClick = onOpenQueue) {
			Icon(
				Icons.AutoMirrored.Filled.List,
				"Cola",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		Text(
			text = fileName,
			color = Color.White.copy(alpha = VideoMaxDimens.alphaHigh),
			style = MaterialTheme.typography.titleMedium,
			maxLines = 1,
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = VideoMaxDimens.spacingSm)
		)
		Spacer(modifier = Modifier.width(72.dp))
	}
}

@Composable
internal fun PlayerSideActions(
	isMuted: Boolean,
	isFavorite: Boolean,
	isOrientationLocked: Boolean,
	isAutoPip: Boolean,
	playbackSpeed: Float,
	onCycleOrientation: () -> Unit,
	onToggleMute: () -> Unit,
	onScreenshot: () -> Unit,
	onToggleAutoPip: () -> Unit,
	onCycleSpeed: () -> Unit,
	onOpenSettings: () -> Unit,
	onToggleFavorite: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm),
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
			.background(
				Color.Black.copy(alpha = VideoMaxDimens.alphaOverlay),
				RoundedCornerShape(VideoMaxDimens.radiusMd)
			)
			.padding(vertical = VideoMaxDimens.spacingMd, horizontal = VideoMaxDimens.spacingSm)
	) {
		RippleIconButton(onClick = onCycleOrientation, size = 44.dp) {
			Icon(
				Icons.Default.ScreenRotation,
				if (isOrientationLocked) "Bloquear orientación" else "Permitir rotación",
				tint = if (isOrientationLocked) MaterialTheme.colorScheme.secondary else Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onToggleMute, size = 44.dp) {
			Icon(
				if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
				"Silenciar",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onScreenshot, size = 44.dp) {
			Icon(
				Icons.Default.Camera,
				"Captura",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onToggleAutoPip, size = 44.dp) {
			Icon(
				Icons.Default.PictureInPicture,
				contentDescription = "PiP",
				tint = if (isAutoPip) MaterialTheme.colorScheme.primary else Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onCycleSpeed, size = 44.dp) {
			Icon(
				Icons.Default.Speed,
				contentDescription = "Velocidad",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onOpenSettings, size = 44.dp) {
			Icon(
				Icons.Default.Settings,
				contentDescription = "Ajustes del video",
				tint = Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
		RippleIconButton(onClick = onToggleFavorite, size = 44.dp) {
			Icon(
				if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
				"Favorito",
				tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerTimelineBar(
	progressState: StateFlow<PlayerProgressState>,
	onSeekFraction: (Float) -> Unit
) {
	val progress by progressState.collectAsStateWithLifecycle()
	val duration = progress.durationMs
	val position = progress.positionMs

	val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
	val animatedFraction by animateFloatAsState(
		targetValue = fraction,
		animationSpec = tween(180),
		label = "progress"
	)

	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.padding(bottom = VideoMaxDimens.spacingXxs)
	) {
		Text(
			Formatters.formatDuration(position),
			color = Color.White.copy(alpha = VideoMaxDimens.alphaMedium),
			style = MaterialTheme.typography.labelSmall
		)
		Slider(
			value = animatedFraction,
			onValueChange = onSeekFraction,
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = VideoMaxDimens.spacingSm),
			thumb = {
				androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
				Box(
					modifier = Modifier
						.width(5.dp)
						.height(24.dp)
						.clip(RoundedCornerShape(2.dp))
						.background(Color.White)
				)
			},
			track = { sliderState ->
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(6.dp)
						.clip(RoundedCornerShape(3.dp))
						.background(Color.White.copy(alpha = 0.2f))
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth(fraction = sliderState.value.coerceIn(0f, 1f))
							.fillMaxSize()
							.clip(RoundedCornerShape(3.dp))
							.background(Color.White)
					)
				}
			},
			colors = SliderDefaults.colors(
				thumbColor = Color.Transparent,
				activeTrackColor = Color.Transparent,
				inactiveTrackColor = Color.Transparent
			)
		)
		Text(
			Formatters.formatDuration(duration),
			color = Color.White.copy(alpha = VideoMaxDimens.alphaMedium),
			style = MaterialTheme.typography.labelSmall
		)
	}
}

@Composable
internal fun PlayerTransportRow(
	isPlaying: Boolean,
	isLocked: Boolean,
	nextEnabled: Boolean,
	onPrevious: () -> Unit,
	onNext: () -> Unit,
	onTogglePlayPause: () -> Unit,
	onToggleLock: () -> Unit,
	onCycleResize: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = VideoMaxDimens.spacingXs),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		RippleIconButton(onClick = onToggleLock, size = 48.dp) {
			Icon(
				if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
				"Bloquear",
				tint = Color.White.copy(alpha = if (isLocked) VideoMaxDimens.alphaHigh else VideoMaxDimens.alphaMedium),
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXxl)
		) {
			RippleIconButton(onClick = onPrevious, size = 48.dp) {
				Icon(
					Icons.Default.SkipPrevious,
					"Anterior",
					tint = Color.White,
					modifier = Modifier.size(VideoMaxDimens.iconSizeLg)
				)
			}
			IconButton(
				onClick = onTogglePlayPause,
				modifier = Modifier
					.size(72.dp)
					.background(Color.White, CircleShape)
			) {
				Icon(
					if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
					if (isPlaying) "Pausar" else "Reproducir",
					tint = Color.Black,
					modifier = Modifier.size(38.dp)
				)
			}
			RippleIconButton(onClick = onNext, size = 48.dp) {
				Icon(
					Icons.Default.SkipNext,
					"Siguiente",
					tint = if (nextEnabled) Color.White else Color.White.copy(VideoMaxDimens.alphaDisabled),
					modifier = Modifier.size(VideoMaxDimens.iconSizeLg)
				)
			}
		}

		RippleIconButton(onClick = onCycleResize, size = 48.dp) {
			Icon(
				Icons.Default.AspectRatio,
				"Tamaño de pantalla",
				tint = Color.White.copy(alpha = VideoMaxDimens.alphaMedium),
				modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
			)
		}
	}
}

@Composable
internal fun RippleIconButton(
	onClick: () -> Unit,
	size: androidx.compose.ui.unit.Dp = 36.dp,
	content: @Composable () -> Unit
) {
	IconButton(
		onClick = onClick,
		interactionSource = remember { MutableInteractionSource() },
		modifier = Modifier
			.size(size)
			.clip(CircleShape)
	) {
		content()
	}
}
