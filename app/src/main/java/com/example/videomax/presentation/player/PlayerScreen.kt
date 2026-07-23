package com.example.videomax.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.PixelCopy
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.videomax.util.Formatters
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Modern, minimal, fluid player screen.
 *
 * NOTE: This screen expects the following members on [PlayerViewModel] / [PlayerUiState]
 * beyond what previously existed, since the new controls need them:
 *   - state.isMuted: Boolean
 *   - viewModel.toggleMute()
 *   - viewModel.zoomScale: Float / viewModel.setZoom(scale: Float) (or keep zoom purely local, as done below)
 *   - onOpenQueue / onCast: wire these to your own queue sheet and cast session as needed.
 * If any of these aren't present yet in your ViewModel, add them — the calls below are
 * written against the names used elsewhere in this file for consistency.
 */
@Composable
fun PlayerScreen(
	onBack: () -> Unit,
	onOpenDetails: (Long) -> Unit,
	onOpenQueue: () -> Unit = {},
	onCast: () -> Unit = {},
	viewModel: PlayerViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val activity = context as Activity
	val audioManager = remember { context.getSystemService(AudioManager::class.java) }
	val scope = rememberCoroutineScope()

	var resizeMode by remember { androidx.compose.runtime.mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
	var dragAccum by remember { mutableFloatStateOf(0f) }
	var zoomScale by remember { mutableFloatStateOf(1f) }
	var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

	val subtitlePicker = rememberLauncherForActivityResult(
		ActivityResultContracts.OpenDocument()
	) { uri: Uri? ->
		uri ?: return@rememberLauncherForActivityResult
		runCatching {
			context.contentResolver.takePersistableUriPermission(
				uri,
				android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
			)
		}
	}

	DisposableEffect(Unit) {
		val window = activity.window
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		val previousOrientation = activity.requestedOrientation
		WindowCompat.setDecorFitsSystemWindows(window, false)
		controller.hide(WindowInsetsCompat.Type.systemBars())
		controller.systemBarsBehavior =
			WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
		val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
		viewModel.setVolumeFraction(currentVolume / maxVolume.toFloat(), fromGesture = false)

		val lp = window.attributes
		if (lp.screenBrightness >= 0f) {
			viewModel.syncBrightnessWithoutHint(lp.screenBrightness)
		}

		onDispose {
			scope.launch { viewModel.persistProgress() }
			activity.requestedOrientation = previousOrientation
			controller.show(WindowInsetsCompat.Type.systemBars())
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			val attrs = window.attributes
			attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
			window.attributes = attrs
		}
	}

	LaunchedEffect(state.orientation) {
		activity.requestedOrientation = when (state.orientation) {
			PlayerOrientation.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
			PlayerOrientation.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
			PlayerOrientation.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		}
	}

	LaunchedEffect(state.brightness, state.brightnessOverrideEnabled) {
		if (!state.brightnessOverrideEnabled) return@LaunchedEffect
		val attrs = activity.window.attributes
		attrs.screenBrightness = state.brightness
		activity.window.attributes = attrs
	}

	LaunchedEffect(state.volumeFraction) {
		val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
		audioManager.setStreamVolume(
			AudioManager.STREAM_MUSIC,
			(state.volumeFraction * max).toInt().coerceIn(0, max),
			0
		)
	}

	BackHandler {
		scope.launch {
			viewModel.persistProgress()
			onBack()
		}
	}

	val nextEnabled = state.hasNext || state.repeatMode != QueueRepeatMode.OFF
	val lifecycleOwner = LocalLifecycleOwner.current
	val latestAutoPip by rememberUpdatedState(state.autoPip)
	val latestIsPlaying by rememberUpdatedState(state.isPlaying)
	val latestVideo by rememberUpdatedState(state.video)

	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_STOP &&
				latestAutoPip &&
				latestIsPlaying &&
				!activity.isInPictureInPictureMode
			) {
				val video = latestVideo
				enterPip(activity, video?.width ?: 16, video?.height ?: 9)
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	val brightnessRef = rememberUpdatedState(state.brightness)
	val volumeRef = rememberUpdatedState(state.volumeFraction)
	val lockedRef = rememberUpdatedState(state.isLocked)
	val gesturesRef = rememberUpdatedState(state.gesturesEnabled)

	val animatedZoom by animateFloatAsState(
		targetValue = zoomScale,
		animationSpec = tween(150),
		label = "zoom"
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black)
	) {
		AndroidView(
			factory = { ctx ->
				PlayerView(ctx).apply {
					player = viewModel.player
					useController = false
					this.resizeMode = resizeMode
					setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
					playerViewRef = this
				}
			},
			update = { it.resizeMode = resizeMode },
			modifier = Modifier
				.fillMaxSize()
				.scale(animatedZoom)
				.pointerInput(Unit) {
					detectTapGestures(
						onTap = { viewModel.toggleControls() },
						onDoubleTap = { offset ->
							if (lockedRef.value) return@detectTapGestures
							val third = size.width / 3f
							when {
								offset.x < third -> viewModel.seekBy(-viewModel.seekStepMs())
								offset.x > third * 2 -> viewModel.seekBy(viewModel.seekStepMs())
								else -> viewModel.togglePlayPause()
							}
						}
					)
				}
				.then(
					if (state.isLocked) Modifier
					else Modifier.pointerInput(Unit) {
						detectTransformGestures { _, _, zoom, _ ->
							zoomScale = (zoomScale * zoom).coerceIn(1f, 3f)
						}
					}
				)
				.then(
					if (state.isLocked || !state.gesturesEnabled) Modifier
					else Modifier
						.pointerInput(Unit) {
							detectVerticalDragGestures(
								onDragEnd = { dragAccum = 0f },
								onVerticalDrag = { change, dragAmount ->
									change.consume()
									val isLeft = change.position.x < size.width / 2f
									val delta = -dragAmount / size.height.toFloat()
									if (isLeft) {
										// Left side vertical swipe -> brightness
										viewModel.setBrightness(
											brightnessRef.value + delta,
											fromGesture = true
										)
									} else {
										// Right side vertical swipe -> volume
										viewModel.setVolumeFraction(
											volumeRef.value + delta,
											fromGesture = true
										)
									}
								}
							)
						}
						.pointerInput(Unit) {
							detectHorizontalDragGestures(
								onDragEnd = { dragAccum = 0f },
								onHorizontalDrag = { change, dragAmount ->
									change.consume()
									if (lockedRef.value || !gesturesRef.value) return@detectHorizontalDragGestures
									dragAccum += dragAmount
									val seekDelta = (dragAccum / size.width * 90_000).toLong()
									if (kotlin.math.abs(seekDelta) > 500) {
										viewModel.seekBy(seekDelta)
										dragAccum = 0f
									}
								}
							)
						}
				)
		)

		PlayerGestureHint(
			hint = state.gestureHint,
			modifier = Modifier.align(Alignment.Center)
		)

		AnimatedVisibility(
			visible = state.controlsVisible,
			enter = fadeIn(tween(220)),
			exit = fadeOut(tween(220))
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				AnimatedVisibility(
					visible = true,
					enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 },
					exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 },
					modifier = Modifier.align(Alignment.TopCenter)
				) {
					PlayerTopBar(
						fileName = state.video?.displayName.orEmpty(),
						isMuted = state.volumeFraction <= 0f,
						onBack = {
							scope.launch {
								viewModel.persistProgress()
								onBack()
							}
						},
						onOpenQueue = onOpenQueue,
						onCycleOrientation = viewModel::cycleOrientation,
						onToggleMute = {
							if (state.volumeFraction > 0f) {
								viewModel.setVolumeFraction(0f, fromGesture = false)
							} else {
								viewModel.setVolumeFraction(0.5f, fromGesture = false)
							}
						},
						onScreenshot = {
							captureFrame(activity, playerViewRef) { /* bitmap saved via callback below */ }
						},
						onCast = onCast
					)
				}

				if (!state.isLocked) {
					AnimatedVisibility(
						visible = true,
						enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
						exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 2 },
						modifier = Modifier.align(Alignment.BottomCenter)
					) {
						Column(
							modifier = Modifier
								.fillMaxWidth()
								.background(
									Brush.verticalGradient(
										listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
									)
								)
								.navigationBarsPadding()
								.padding(horizontal = 14.dp, vertical = 6.dp)
						) {
							PlayerTimelineBar(
								progressState = viewModel.progressState,
								onSeekFraction = { fraction ->
									val duration = viewModel.progressState.value.durationMs
									if (duration > 0) {
										viewModel.seekTo((fraction * duration).toLong())
									}
								}
							)

							PlayerTransportRow(
								isPlaying = state.isPlaying,
								isLocked = state.isLocked,
								nextEnabled = nextEnabled,
								onPrevious = viewModel::playPrevious,
								onNext = viewModel::playNext,
								onTogglePlayPause = viewModel::togglePlayPause,
								onToggleLock = viewModel::toggleLock,
								onCycleResize = {
									resizeMode = when (resizeMode) {
										AspectRatioFrameLayout.RESIZE_MODE_FIT ->
											AspectRatioFrameLayout.RESIZE_MODE_ZOOM
										AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
											AspectRatioFrameLayout.RESIZE_MODE_FILL
										else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
									}
								}
							)
						}
					}
				} else if (state.controlsVisible) {
					Text(
						text = "Pantalla bloqueada — toca el candado para desbloquear",
						color = Color.White.copy(alpha = 0.8f),
						style = MaterialTheme.typography.labelMedium,
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.navigationBarsPadding()
							.padding(16.dp)
					)
					IconButton(
						onClick = viewModel::toggleLock,
						modifier = Modifier
							.align(Alignment.CenterEnd)
							.padding(24.dp)
							.size(48.dp)
							.background(Color.Black.copy(alpha = 0.45f), CircleShape)
					) {
						Icon(Icons.Default.Lock, "Desbloquear", tint = Color.White)
					}
				}
			}
		}
	}
}

@Composable
private fun PlayerGestureHint(
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

/**
 * Top bar: back, queue/list, centered filename, rotate, mute, screenshot, cast.
 */
@Composable
private fun PlayerTopBar(
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
				if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
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

/**
 * Progress row: current time, animated draggable slider with white thumb, total duration.
 */
@Composable
private fun PlayerTimelineBar(
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

/**
 * Below the progress bar: lock, previous, big play/pause, next, screen-size toggle.
 */
@Composable
private fun PlayerTransportRow(
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
private fun RippleIconButton(
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

private fun enterPip(activity: Activity, width: Int, height: Int) {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
	val w = width.coerceAtLeast(1)
	val h = height.coerceAtLeast(1)
	val params = PictureInPictureParams.Builder()
		.setAspectRatio(Rational(w, h))
		.build()
	activity.enterPictureInPictureMode(params)
}

/**
 * Captures the current video frame from [playerView]'s surface using PixelCopy.
 * Requires API 24+. Hook the resulting Bitmap up to your own save/share flow.
 */
private fun captureFrame(
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