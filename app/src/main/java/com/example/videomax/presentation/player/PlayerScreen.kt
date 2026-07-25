package com.example.videomax.presentation.player

import android.app.Activity
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.videomax.domain.model.Video
import com.example.videomax.presentation.components.VideoThumbnail
import com.example.videomax.util.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
	fun PlayerScreen(
	onBack: () -> Unit,
	onOpenDetails: (Long) -> Unit,
	viewModel: PlayerViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val activity = context as Activity
	val audioManager = remember { context.getSystemService(android.media.AudioManager::class.java) }
	val scope = rememberCoroutineScope()

	var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
	var dragAccum by remember { mutableFloatStateOf(0f) }
	var zoomScale by remember { mutableFloatStateOf(1f) }
	var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
	var showQueueSheet by remember { mutableStateOf(false) }
	var showVideoSettings by remember { mutableStateOf(false) }

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

	SetupSystemBars(activity, audioManager, viewModel, scope)
	SetupLifecyclePauser(activity, viewModel, state)

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
		val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
		audioManager.setStreamVolume(
			android.media.AudioManager.STREAM_MUSIC,
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

	SetupAutoPip(activity, state)

	val brightnessRef = rememberUpdatedState(state.brightness)
	val volumeRef = rememberUpdatedState(state.volumeFraction)
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
		PlayerVideoSurface(
			viewModel = viewModel,
			resizeMode = resizeMode,
			animatedZoom = animatedZoom,
			state = state,
			dragAccum = dragAccum,
			brightnessRef = brightnessRef,
			volumeRef = volumeRef,
			gesturesRef = gesturesRef,
			onDragAccumChange = { dragAccum = it },
			onZoomChange = { zoomScale = it },
			onPlayerViewRef = { playerViewRef = it }
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
			PlayerControlsOverlay(
				state = state,
				viewModel = viewModel,
				activity = activity,
				playerViewRef = playerViewRef,
				resizeMode = resizeMode,
				scope = scope,
				onBack = onBack,
				onOpenQueue = { showQueueSheet = true },
				onScreenshot = {
					captureFrame(activity, playerViewRef) { bitmap ->
						if (bitmap != null && saveBitmapToGallery(activity, bitmap)) {
							scope.launch {
								Toast.makeText(context, "Captura guardada en Galería", Toast.LENGTH_SHORT).show()
							}
						} else {
							scope.launch {
								Toast.makeText(context, "Error al guardar captura", Toast.LENGTH_SHORT).show()
							}
						}
					}
				},
				onToggleFavorite = viewModel::toggleFavorite,
				onResizeModeChange = { resizeMode = it },
				onOpenSettings = { showVideoSettings = true }
			)
		}
	}

	if (showQueueSheet) {
		PlayerQueueSheet(
			viewModel = viewModel,
			onDismiss = { showQueueSheet = false }
		)
	}

	if (showVideoSettings) {
		VideoSettingsSheet(
			viewModel = viewModel,
			state = state,
			onDismiss = { showVideoSettings = false }
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerQueueSheet(
	viewModel: PlayerViewModel,
	onDismiss: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState()
	val scope = rememberCoroutineScope()
	val queueIds = viewModel.getQueueIds()
	val currentIndex = viewModel.getCurrentQueueIndex()
	val currentVideoId = viewModel.getCurrentVideoId()
	val repeatMode = viewModel.uiState.collectAsStateWithLifecycle().value.repeatMode
	var queueVideos by remember { mutableStateOf<List<Video>>(emptyList()) }
	var shuffleTrigger by remember { mutableIntStateOf(0) }

	LaunchedEffect(queueIds, shuffleTrigger) {
		queueVideos = viewModel.getQueueVideos()
	}

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 32.dp)
		) {
			Text(
				text = "Cola de reproducción (${queueIds.size})",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
			)

			Row(
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 20.dp, vertical = 4.dp)
			) {
				FilledTonalButton(
					onClick = {
						scope.launch {
							viewModel.shuffleQueue()
							shuffleTrigger++
						}
					},
					enabled = queueIds.size >= 2
				) {
					Icon(
						imageVector = Icons.Default.Shuffle,
						contentDescription = "Aleatorizar",
						modifier = Modifier.size(18.dp)
					)
					Spacer(modifier = Modifier.width(6.dp))
					Text("Aleatorizar", style = MaterialTheme.typography.labelMedium)
				}

				FilledTonalButton(
					onClick = { viewModel.cycleRepeatMode() }
				) {
					Icon(
						imageVector = when (repeatMode) {
							QueueRepeatMode.ONE -> Icons.Default.RepeatOne
							else -> Icons.Default.Repeat
						},
						contentDescription = "Repetir",
						modifier = Modifier.size(18.dp)
					)
					Spacer(modifier = Modifier.width(6.dp))
					Text(
						text = when (repeatMode) {
							QueueRepeatMode.OFF -> "Repetir off"
							QueueRepeatMode.ONE -> "Repetir 1"
							QueueRepeatMode.ALL -> "Repetir all"
						},
						style = MaterialTheme.typography.labelMedium
					)
				}
			}

			if (queueIds.isEmpty()) {
				Text(
					text = "Cola vacía",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
				)
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxWidth()
				) {
					items(queueIds.size) { index ->
						val videoId = queueIds[index]
						val isCurrent = videoId == currentVideoId
						val video = queueVideos.find { it.id == videoId }

						Row(
							modifier = Modifier
								.fillMaxWidth()
								.background(
									if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
									else Color.Transparent
								)
								.clickable {
									viewModel.playQueueItem(index)
								}
								.padding(horizontal = 20.dp, vertical = 8.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Text(
								text = "${index + 1}",
								style = MaterialTheme.typography.labelMedium,
								color = if (isCurrent) MaterialTheme.colorScheme.primary
								else MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.width(28.dp)
							)

							Box(
								modifier = Modifier
									.size(width = 64.dp, height = 36.dp)
									.clip(RoundedCornerShape(6.dp))
									.background(MaterialTheme.colorScheme.surfaceVariant)
							) {
								if (video != null) {
									VideoThumbnail(
										uri = video.uri,
										cacheKey = "q_${video.id}",
										lightweight = true,
										modifier = Modifier.fillMaxSize()
									)
								}
							}

							Spacer(modifier = Modifier.width(12.dp))

							Column(modifier = Modifier.weight(1f)) {
								Text(
									text = video?.displayName ?: "Video $videoId",
									style = MaterialTheme.typography.bodyMedium,
									color = if (isCurrent) MaterialTheme.colorScheme.primary
									else MaterialTheme.colorScheme.onSurface,
									maxLines = 1,
									overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
								)
								if (video != null && video.durationMs > 0) {
									Text(
										text = Formatters.formatDuration(video.durationMs),
										style = MaterialTheme.typography.labelSmall,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
								}
							}

							if (isCurrent) {
								Icon(
									imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.primary,
									modifier = Modifier.size(20.dp)
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun SetupSystemBars(
	activity: Activity,
	audioManager: android.media.AudioManager,
	viewModel: PlayerViewModel,
	scope: kotlinx.coroutines.CoroutineScope
) {
	DisposableEffect(Unit) {
		val window = activity.window
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		val previousOrientation = activity.requestedOrientation
		WindowCompat.setDecorFitsSystemWindows(window, false)
		controller.hide(WindowInsetsCompat.Type.systemBars())
		controller.systemBarsBehavior =
			WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
		val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
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
}

@Composable
private fun SetupAutoPip(activity: Activity, state: PlayerUiState) {
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
}

/**
 * Pauses the player and persists position when the Activity goes to background (ON_STOP)
 * or is being destroyed (ON_DESTROY). Prevents audio/video from playing in background.
 * If autoPiP is enabled, ON_STOP is deferred briefly to allow PiP transition to complete.
 * ON_DESTROY always cleans up as a safety net.
 */
@Composable
private fun SetupLifecyclePauser(activity: Activity, viewModel: PlayerViewModel, state: PlayerUiState) {
	val lifecycleOwner = LocalLifecycleOwner.current
	val latestAutoPip by rememberUpdatedState(state.autoPip)
	val scope = rememberCoroutineScope()

	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			when (event) {
				Lifecycle.Event.ON_STOP -> {
					if (latestAutoPip) {
						// Give PiP transition time to activate; if it didn't, pause anyway.
						scope.launch {
							delay(300)
							if (!activity.isInPictureInPictureMode) {
								viewModel.pauseAndPersist()
							}
						}
					} else {
						viewModel.pauseAndPersist()
					}
				}
				Lifecycle.Event.ON_DESTROY -> {
					viewModel.pauseAndPersist()
				}
				else -> { /* no-op */ }
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}
}

@Composable
private fun PlayerVideoSurface(
	viewModel: PlayerViewModel,
	resizeMode: Int,
	animatedZoom: Float,
	state: PlayerUiState,
	dragAccum: Float,
	brightnessRef: androidx.compose.runtime.State<Float>,
	volumeRef: androidx.compose.runtime.State<Float>,
	gesturesRef: androidx.compose.runtime.State<Boolean>,
	onDragAccumChange: (Float) -> Unit,
	onZoomChange: (Float) -> Unit,
	onPlayerViewRef: (PlayerView) -> Unit
) {
	var localDragAccum by remember { mutableFloatStateOf(dragAccum) }
	var localZoom by remember { mutableFloatStateOf(1f) }

	AndroidView(
		factory = { ctx ->
			PlayerView(ctx).apply {
				player = viewModel.player
				useController = false
				this.resizeMode = resizeMode
				setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
				onPlayerViewRef(this)
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
						if (state.isLocked) return@detectTapGestures
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
						localZoom = (localZoom * zoom).coerceIn(1f, 3f)
						onZoomChange(localZoom)
					}
				}
			)
			.then(
				if (state.isLocked || !state.gesturesEnabled) Modifier
				else Modifier
					.pointerInput(Unit) {
						detectVerticalDragGestures(
							onDragEnd = { localDragAccum = 0f; onDragAccumChange(0f) },
							onVerticalDrag = { change, dragAmount ->
								change.consume()
								val isLeft = change.position.x < size.width / 2f
								val delta = -dragAmount / size.height.toFloat()
								if (isLeft) {
									viewModel.setBrightness(
										brightnessRef.value + delta,
										fromGesture = true
									)
								} else {
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
							onDragEnd = { localDragAccum = 0f; onDragAccumChange(0f) },
							onHorizontalDrag = { change, dragAmount ->
								change.consume()
								if (state.isLocked || !gesturesRef.value) return@detectHorizontalDragGestures
								localDragAccum += dragAmount
								onDragAccumChange(localDragAccum)
								val seekDelta = (localDragAccum / size.width * 90_000).toLong()
								if (kotlin.math.abs(seekDelta) > 500) {
									viewModel.seekBy(seekDelta)
									localDragAccum = 0f
									onDragAccumChange(0f)
								}
							}
						)
					}
			)
	)
}

@Composable
private fun PlayerControlsOverlay(
	state: PlayerUiState,
	viewModel: PlayerViewModel,
	activity: Activity,
	playerViewRef: PlayerView?,
	resizeMode: Int,
	scope: kotlinx.coroutines.CoroutineScope,
	onBack: () -> Unit,
	onOpenQueue: () -> Unit,
	onScreenshot: () -> Unit,
	onToggleFavorite: () -> Unit,
	onResizeModeChange: (Int) -> Unit,
	onOpenSettings: () -> Unit
) {
	val nextEnabled = state.hasNext || state.repeatMode != QueueRepeatMode.OFF
	var showSpeedMenu by remember { mutableStateOf(false) }

	Box(modifier = Modifier.fillMaxSize()) {
		AnimatedVisibility(
			visible = true,
			enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 },
			exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 },
			modifier = Modifier.align(Alignment.TopCenter)
		) {
			PlayerTopBar(
				fileName = state.video?.displayName.orEmpty(),
				onBack = {
					scope.launch {
						viewModel.persistProgress()
						onBack()
					}
				},
				onOpenQueue = onOpenQueue
			)
		}

		AnimatedVisibility(
			visible = true,
			enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 },
			exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 },
			modifier = Modifier
				.align(Alignment.TopEnd)
				.statusBarsPadding()
				.padding(top = 4.dp, end = 6.dp)
		) {
			PlayerSideActions(
				isMuted = state.volumeFraction <= 0f,
				isFavorite = state.video?.isFavorite == true,
				isOrientationLocked = state.orientation != PlayerOrientation.AUTO,
				onCycleOrientation = {
					val isLandscape = activity.resources.configuration.orientation ==
						android.content.res.Configuration.ORIENTATION_LANDSCAPE
					viewModel.cycleOrientation(isLandscape)
				},
				onToggleMute = {
					if (state.volumeFraction > 0f) {
						viewModel.setVolumeFraction(0f, fromGesture = false)
					} else {
						viewModel.setVolumeFraction(0.5f, fromGesture = false)
					}
				},
				onScreenshot = onScreenshot,
				onToggleFavorite = onToggleFavorite
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
					PlayerOptionsRow(
						isAutoPip = state.autoPip,
						playbackSpeed = state.playbackSpeed,
						onToggleAutoPip = viewModel::toggleAutoPip,
						onCycleSpeed = { showSpeedMenu = true },
						onOpenSettings = onOpenSettings
					)

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
							onResizeModeChange(
								when (resizeMode) {
									AspectRatioFrameLayout.RESIZE_MODE_FIT ->
										AspectRatioFrameLayout.RESIZE_MODE_ZOOM
									AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
										AspectRatioFrameLayout.RESIZE_MODE_FILL
									else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
								}
							)
						}
					)
				}
			}

			val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
			DropdownMenu(
				expanded = showSpeedMenu,
				onDismissRequest = { showSpeedMenu = false }
			) {
				speeds.forEach { speed ->
					DropdownMenuItem(
						text = {
							Text(
								text = "${speed}x",
								style = MaterialTheme.typography.bodyMedium,
								color = if (speed == state.playbackSpeed) MaterialTheme.colorScheme.primary
								else MaterialTheme.colorScheme.onSurface
							)
						},
						onClick = {
							viewModel.setSpeed(speed)
							showSpeedMenu = false
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
					.background(Color.Black.copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape)
			) {
				Icon(Icons.Default.Lock, "Desbloquear", tint = Color.White)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSettingsSheet(
	viewModel: PlayerViewModel,
	state: PlayerUiState,
	onDismiss: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState()
	val context = LocalContext.current

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		containerColor = MaterialTheme.colorScheme.surface
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 32.dp)
		) {
			Text(
				text = "Ajustes del video",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
			)

			VideoSettingRow(
				icon = Icons.Default.PlayCircle,
				title = "Reproducción en fondo",
				subtitle = "Seguir reproduciendo con pantalla apagada"
			)
			VideoSettingRow(
				icon = Icons.Default.Equalizer,
				title = "Ecualizador",
				subtitle = "Ajustar audio"
			)
			VideoSettingRow(
				icon = Icons.Default.DarkMode,
				title = "Modo nocturno",
				subtitle = "Reducir brillo y calor de color"
			)
			VideoSettingRow(
				icon = Icons.Default.Timer,
				title = "Temporizador",
				subtitle = "Detener reproducción automáticamente"
			)
			VideoSettingRow(
				icon = Icons.Default.Flip,
				title = "Espejo",
				subtitle = "Volcar imagen horizontalmente"
			)
			VideoSettingRow(
				icon = Icons.Default.Repeat,
				title = "Bucle",
				subtitle = when (state.repeatMode) {
					QueueRepeatMode.OFF -> "Desactivado"
					QueueRepeatMode.ONE -> "Repetir uno"
					QueueRepeatMode.ALL -> "Repetir todo"
				},
				isActive = state.repeatMode != QueueRepeatMode.OFF,
				onClick = { viewModel.cycleRepeatMode() }
			)
			VideoSettingRow(
				icon = Icons.Default.Shuffle,
				title = "Aleatorio",
				subtitle = "Orden aleatorio de la cola",
				onClick = {
					viewModel.playQueueItem(0)
				}
			)
			VideoSettingRow(
				icon = Icons.Default.Info,
				title = "Propiedades",
				subtitle = state.video?.let { "${it.width}x${it.height} · ${Formatters.formatFileSize(it.sizeBytes)}" } ?: ""
			)
			VideoSettingRow(
				icon = Icons.Default.Share,
				title = "Compartir",
				subtitle = "Enviar video por otra app",
				onClick = {
					state.video?.let {
						val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
							type = it.mimeType
							addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
							putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(it.uri))
						}
						context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir video"))
					}
					onDismiss()
				}
			)
			VideoSettingRow(
				icon = Icons.Default.Delete,
				title = "Eliminar",
				subtitle = "Borrar este video del dispositivo",
				isDestructive = true,
				onClick = { onDismiss() }
			)
		}
	}
}

@Composable
private fun VideoSettingRow(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	title: String,
	subtitle: String,
	isActive: Boolean = false,
	isDestructive: Boolean = false,
	onClick: (() -> Unit)? = null
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
			.padding(horizontal = 20.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = title,
			tint = when {
				isDestructive -> MaterialTheme.colorScheme.error
				isActive -> MaterialTheme.colorScheme.primary
				else -> MaterialTheme.colorScheme.onSurfaceVariant
			},
			modifier = Modifier.size(22.dp)
		)
		Spacer(modifier = Modifier.width(16.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyLarge,
				color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (isActive) {
			Icon(
				imageVector = Icons.Default.Check,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(18.dp)
			)
		}
	}
}
