package com.example.videomax.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.videomax.domain.model.SubtitleTrack
import com.example.videomax.domain.model.Video
import com.example.videomax.util.Formatters
import com.example.videomax.util.SubtitleHelper
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
	onBack: () -> Unit,
	onOpenDetails: (Long) -> Unit,
	viewModel: PlayerViewModel = hiltViewModel()
) {
	// Infrequent chrome state — does NOT include the 500ms timeline poll.
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val activity = context as Activity
	val audioManager = remember {
		context.getSystemService(AudioManager::class.java)
	}
	val scope = rememberCoroutineScope()

	var subtitleMenu by remember { mutableStateOf(false) }
	var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
	var dragAccum by remember { mutableFloatStateOf(0f) }

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
		val mime = SubtitleHelper.mimeForUri(context, uri)
		viewModel.addExternalSubtitle(uri, mime, uri.lastPathSegment ?: "Subtitle")
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
			PlayerOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
			PlayerOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
			PlayerOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
				}
			},
			update = { it.resizeMode = resizeMode },
			modifier = Modifier
				.fillMaxSize()
				.pointerInput(Unit) {
					detectTapGestures(
						onTap = { viewModel.toggleControls() },
						onDoubleTap = { offset ->
							if (lockedRef.value) return@detectTapGestures
							val mid = size.width / 2
							if (offset.x < mid) viewModel.seekBy(-viewModel.seekStepMs())
							else viewModel.seekBy(viewModel.seekStepMs())
						}
					)
				}
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
			enter = fadeIn(),
			exit = fadeOut()
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				PlayerTopBar(
					video = state.video,
					queueIndex = state.queueIndex,
					queueSize = state.queueSize,
					isLocked = state.isLocked,
					onBack = {
						scope.launch {
							viewModel.persistProgress()
							onBack()
						}
					},
					onCycleOrientation = viewModel::cycleOrientation,
					onToggleFavorite = viewModel::toggleFavorite,
					onOpenDetails = { state.video?.id?.let(onOpenDetails) },
					onToggleLock = viewModel::toggleLock
				)

				if (!state.isLocked) {
					Column(
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.background(
								Brush.verticalGradient(
									listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
								)
							)
							.navigationBarsPadding()
							.padding(horizontal = 12.dp, vertical = 10.dp)
					) {
						// Only this subtree collects the 500ms progress flow.
						PlayerTimelineBar(
							progressState = viewModel.progressState,
							onSeekFraction = { fraction ->
								val duration = viewModel.progressState.value.durationMs
								if (duration > 0) {
									viewModel.seekTo((fraction * duration).toLong())
								}
							}
						)

						PlayerTransportControls(
							isPlaying = state.isPlaying,
							nextEnabled = nextEnabled,
							onSeekBack = { viewModel.seekBy(-viewModel.seekStepMs()) },
							onSeekForward = { viewModel.seekBy(viewModel.seekStepMs()) },
							onPrevious = viewModel::playPrevious,
							onNext = viewModel::playNext,
							onTogglePlayPause = viewModel::togglePlayPause
						)

						PlayerSecondaryControls(
							playbackSpeed = state.playbackSpeed,
							repeatMode = state.repeatMode,
							subtitleTracks = state.subtitleTracks,
							subtitleMenuExpanded = subtitleMenu,
							onSubtitleMenuChange = { subtitleMenu = it },
							onCycleSpeed = viewModel::cycleSpeed,
							onCycleRepeat = viewModel::cycleRepeatMode,
							onSelectSubtitle = viewModel::selectSubtitle,
							onPickSubtitle = { subtitlePicker.launch(arrayOf("*/*")) },
							onCycleResize = {
								resizeMode = when (resizeMode) {
									AspectRatioFrameLayout.RESIZE_MODE_FIT ->
										AspectRatioFrameLayout.RESIZE_MODE_ZOOM
									AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
										AspectRatioFrameLayout.RESIZE_MODE_FILL
									else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
								}
							},
							onPip = {
								enterPip(
									activity,
									state.video?.width ?: 16,
									state.video?.height ?: 9
								)
							}
						)
					}
				} else if (state.controlsVisible) {
					Text(
						text = "Screen locked — tap lock to unlock",
						color = Color.White.copy(alpha = 0.8f),
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.navigationBarsPadding()
							.padding(24.dp)
					)
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
		enter = fadeIn(),
		exit = fadeOut(),
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
private fun PlayerTopBar(
	video: Video?,
	queueIndex: Int,
	queueSize: Int,
	isLocked: Boolean,
	onBack: () -> Unit,
	onCycleOrientation: () -> Unit,
	onToggleFavorite: () -> Unit,
	onOpenDetails: () -> Unit,
	onToggleLock: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(
				Brush.verticalGradient(
					listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
				)
			)
			.statusBarsPadding()
			.padding(horizontal = 4.dp, vertical = 2.dp)
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth()
		) {
			IconButton(onClick = onBack) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
			}
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = video?.displayName.orEmpty(),
					color = Color.White,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 1
				)
				if (queueSize > 1) {
					Text(
						text = "${queueIndex + 1} / $queueSize",
						color = Color.White.copy(alpha = 0.7f),
						style = MaterialTheme.typography.labelMedium
					)
				}
			}
			if (!isLocked) {
				IconButton(onClick = onCycleOrientation) {
					Icon(Icons.Default.ScreenRotation, null, tint = Color.White)
				}
				IconButton(onClick = onToggleFavorite) {
					Icon(
						if (video?.isFavorite == true) Icons.Default.Favorite
						else Icons.Default.FavoriteBorder,
						null,
						tint = Color.White
					)
				}
				IconButton(onClick = onOpenDetails) {
					Icon(Icons.Default.Info, null, tint = Color.White)
				}
			}
			IconButton(onClick = onToggleLock) {
				Icon(
					if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
					null,
					tint = Color.White
				)
			}
		}
	}
}

/**
 * Isolates high-frequency timeline recompositions from the rest of the player chrome.
 */
@Composable
private fun PlayerTimelineBar(
	progressState: StateFlow<PlayerProgressState>,
	onSeekFraction: (Float) -> Unit
) {
	val progress by progressState.collectAsStateWithLifecycle()
	val duration = progress.durationMs
	val position = progress.positionMs

	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			Formatters.formatDuration(position),
			color = Color.White,
			style = MaterialTheme.typography.labelLarge
		)
		Slider(
			value = if (duration > 0) {
				(position.toFloat() / duration).coerceIn(0f, 1f)
			} else {
				0f
			},
			onValueChange = onSeekFraction,
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = 8.dp),
			colors = SliderDefaults.colors(
				thumbColor = MaterialTheme.colorScheme.primary,
				activeTrackColor = MaterialTheme.colorScheme.primary
			)
		)
		Text(
			Formatters.formatDuration(duration),
			color = Color.White,
			style = MaterialTheme.typography.labelLarge
		)
	}
}

@Composable
private fun PlayerTransportControls(
	isPlaying: Boolean,
	nextEnabled: Boolean,
	onSeekBack: () -> Unit,
	onSeekForward: () -> Unit,
	onPrevious: () -> Unit,
	onNext: () -> Unit,
	onTogglePlayPause: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 6.dp),
		horizontalArrangement = Arrangement.SpaceEvenly,
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onSeekBack) {
			Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(28.dp))
		}
		IconButton(
			onClick = onPrevious,
			modifier = Modifier
				.size(52.dp)
				.background(Color.White.copy(alpha = 0.12f), CircleShape)
		) {
			Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
		}
		IconButton(
			onClick = onTogglePlayPause,
			modifier = Modifier
				.size(68.dp)
				.background(MaterialTheme.colorScheme.primary, CircleShape)
		) {
			Icon(
				if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
				null,
				tint = MaterialTheme.colorScheme.onPrimary,
				modifier = Modifier.size(36.dp)
			)
		}
		IconButton(
			onClick = onNext,
			enabled = nextEnabled,
			modifier = Modifier
				.size(52.dp)
				.background(Color.White.copy(alpha = 0.12f), CircleShape)
		) {
			Icon(
				Icons.Default.SkipNext,
				"Next",
				tint = if (nextEnabled) Color.White else Color.White.copy(0.35f),
				modifier = Modifier.size(32.dp)
			)
		}
		IconButton(onClick = onSeekForward) {
			Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(28.dp))
		}
	}
}

@Composable
private fun PlayerSecondaryControls(
	playbackSpeed: Float,
	repeatMode: QueueRepeatMode,
	subtitleTracks: List<SubtitleTrack>,
	subtitleMenuExpanded: Boolean,
	onSubtitleMenuChange: (Boolean) -> Unit,
	onCycleSpeed: () -> Unit,
	onCycleRepeat: () -> Unit,
	onSelectSubtitle: (Int) -> Unit,
	onPickSubtitle: () -> Unit,
	onCycleResize: () -> Unit,
	onPip: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onCycleSpeed) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(Icons.Default.Speed, null, tint = Color.White)
				Spacer(modifier = Modifier.width(4.dp))
				Text(Formatters.formatSpeed(playbackSpeed), color = Color.White)
			}
		}
		IconButton(onClick = onCycleRepeat) {
			Icon(
				imageVector = when (repeatMode) {
					QueueRepeatMode.ONE -> Icons.Default.RepeatOne
					else -> Icons.Default.Repeat
				},
				contentDescription = "Repeat",
				tint = when (repeatMode) {
					QueueRepeatMode.OFF -> Color.White.copy(alpha = 0.45f)
					else -> MaterialTheme.colorScheme.primary
				}
			)
		}
		Box {
			IconButton(onClick = { onSubtitleMenuChange(true) }) {
				Icon(Icons.Default.Subtitles, null, tint = Color.White)
			}
			DropdownMenu(
				expanded = subtitleMenuExpanded,
				onDismissRequest = { onSubtitleMenuChange(false) }
			) {
				DropdownMenuItem(
					text = { Text("Off") },
					onClick = {
						onSelectSubtitle(-1)
						onSubtitleMenuChange(false)
					}
				)
				subtitleTracks.forEachIndexed { index, track ->
					DropdownMenuItem(
						text = { Text(track.label) },
						onClick = {
							onSelectSubtitle(index)
							onSubtitleMenuChange(false)
						}
					)
				}
				DropdownMenuItem(
					text = { Text("Load SRT / VTT / ASS…") },
					onClick = {
						onSubtitleMenuChange(false)
						onPickSubtitle()
					}
				)
			}
		}
		IconButton(onClick = onCycleResize) {
			Icon(Icons.Default.AspectRatio, null, tint = Color.White)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			IconButton(onClick = onPip) {
				Icon(Icons.Default.PictureInPictureAlt, null, tint = Color.White)
			}
		}
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
