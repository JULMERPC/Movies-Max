package com.example.videomax.presentation.player

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.videomax.util.Formatters
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
	playerView: PlayerView?,
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
	modifier: Modifier = Modifier
) {
	AnimatedVisibility(
		visible = hint != null,
		enter = fadeIn(tween(150)),
		exit = fadeOut(tween(150)),
		modifier = modifier
	) {
		Text(
			text = hint.orEmpty(),
			color = Color.White,
			style = MaterialTheme.typography.titleLarge,
			modifier = Modifier
				.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
				.padding(horizontal = 20.dp, vertical = 12.dp)
		)
	}
}

@Composable
internal fun PlayerTopBar(
	fileName: String,
	isMuted: Boolean,
	onBack: () -> Unit,
	onOpenQueue: () -> Unit,
	onCycleOrientation: () -> Unit,
	onToggleMute: () -> Unit,
	onScreenshot: () -> Unit,
	onCast: () -> Unit
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier
			.fillMaxWidth()
			.background(
				Brush.verticalGradient(
					listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
				)
			)
			.statusBarsPadding()
			.padding(horizontal = 4.dp, vertical = 2.dp)
	) {
		RippleIconButton(onClick = onBack) {
			Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(22.dp))
		}
		RippleIconButton(onClick = onOpenQueue) {
			Icon(Icons.AutoMirrored.Filled.List, "Lista", tint = Color.White, modifier = Modifier.size(20.dp))
		}
		Text(
			text = fileName,
			color = Color.White,
			style = MaterialTheme.typography.labelLarge,
			maxLines = 1,
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = 6.dp)
		)
		RippleIconButton(onClick = onCycleOrientation) {
			Icon(Icons.Default.ScreenRotation, "Rotar", tint = Color.White, modifier = Modifier.size(20.dp))
		}
		RippleIconButton(onClick = onToggleMute) {
			Icon(
				if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
				"Silenciar",
				tint = Color.White,
				modifier = Modifier.size(20.dp)
			)
		}
		RippleIconButton(onClick = onScreenshot) {
			Icon(Icons.Default.Camera, "Captura", tint = Color.White, modifier = Modifier.size(20.dp))
		}
		RippleIconButton(onClick = onCast) {
			Icon(Icons.Default.Cast, "Cast", tint = Color.White, modifier = Modifier.size(20.dp))
		}
	}
}

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
		modifier = Modifier.padding(bottom = 2.dp)
	) {
		Text(
			Formatters.formatDuration(position),
			color = Color.White.copy(alpha = 0.9f),
			style = MaterialTheme.typography.labelSmall
		)
		Slider(
			value = animatedFraction,
			onValueChange = onSeekFraction,
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = 8.dp),
			colors = SliderDefaults.colors(
				thumbColor = Color.White,
				activeTrackColor = Color.White,
				inactiveTrackColor = Color.White.copy(alpha = 0.28f)
			)
		)
		Text(
			Formatters.formatDuration(duration),
			color = Color.White.copy(alpha = 0.9f),
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
			.padding(vertical = 4.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		RippleIconButton(onClick = onToggleLock, size = 40.dp) {
			Icon(
				if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
				"Bloquear",
				tint = Color.White,
				modifier = Modifier.size(20.dp)
			)
		}

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(18.dp)
		) {
			RippleIconButton(onClick = onPrevious, size = 40.dp) {
				Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(26.dp))
			}
			IconButton(
				onClick = onTogglePlayPause,
				modifier = Modifier
					.size(56.dp)
					.background(Color.White, CircleShape)
			) {
				Icon(
					if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
					if (isPlaying) "Pausar" else "Reproducir",
					tint = Color.Black,
					modifier = Modifier.size(30.dp)
				)
			}
			IconButton(
				onClick = onNext,
				enabled = nextEnabled,
				modifier = Modifier.size(40.dp)
			) {
				Icon(
					Icons.Default.SkipNext,
					"Siguiente",
					tint = if (nextEnabled) Color.White else Color.White.copy(0.35f),
					modifier = Modifier.size(26.dp)
				)
			}
		}

		RippleIconButton(onClick = onCycleResize, size = 40.dp) {
			Icon(Icons.Default.AspectRatio, "Tamaño de pantalla", tint = Color.White, modifier = Modifier.size(20.dp))
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
