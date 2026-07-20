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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.videomax.util.Formatters
import com.example.videomax.util.SubtitleHelper
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
	val audioManager = remember {
		context.getSystemService(AudioManager::class.java)
	}
	val scope = rememberCoroutineScope()

	var subtitleMenu by remember { mutableStateOf(false) }
	var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
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
		viewModel.setVolumeFraction(currentVolume / maxVolume.toFloat())

		val lp = window.attributes
		if (lp.screenBrightness < 0f) {
			viewModel.setBrightness(0.5f)
		} else {
			viewModel.setBrightness(lp.screenBrightness)
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

	LaunchedEffect(state.brightness) {
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
				.pointerInput(state.isLocked) {
					detectTapGestures(
						onTap = { viewModel.toggleControls() },
						onDoubleTap = { offset ->
							if (state.isLocked) return@detectTapGestures
							val mid = size.width / 2
							if (offset.x < mid) viewModel.seekBy(-viewModel.seekStepMs())
							else viewModel.seekBy(viewModel.seekStepMs())
						}
					)
				}
				.then(
					if (state.isLocked) Modifier
					else Modifier
						.pointerInput(Unit) {
							detectVerticalDragGestures(
								onDragEnd = {
									viewModel.clearGestureHint()
									dragAccum = 0f
								},
								onVerticalDrag = { change, dragAmount ->
									change.consume()
									val isLeft = change.position.x < size.width / 2f
									val delta = -dragAmount / size.height.toFloat()
									if (isLeft) {
										viewModel.setBrightness(state.brightness + delta)
									} else {
										viewModel.setVolumeFraction(state.volumeFraction + delta)
									}
								}
							)
						}
						.pointerInput(Unit) {
							detectHorizontalDragGestures(
								onDragEnd = {
									viewModel.clearGestureHint()
									dragAccum = 0f
								},
								onHorizontalDrag = { change, dragAmount ->
									change.consume()
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

		AnimatedVisibility(
			visible = state.gestureHint != null,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier.align(Alignment.Center)
		) {
			Text(
				text = state.gestureHint.orEmpty(),
				color = Color.White,
				style = MaterialTheme.typography.headlineMedium,
				modifier = Modifier
					.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
					.padding(horizontal = 20.dp, vertical = 12.dp)
			)
		}

		AnimatedVisibility(
			visible = state.controlsVisible,
			enter = fadeIn(),
			exit = fadeOut()
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.background(
							Brush.verticalGradient(
								listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
							)
						)
						.statusBarsPadding()
						.padding(horizontal = 8.dp, vertical = 4.dp)
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth()
					) {
						IconButton(onClick = {
							scope.launch {
								viewModel.persistProgress()
								onBack()
							}
						}) {
							Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
						}
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = state.video?.displayName.orEmpty(),
								color = Color.White,
								style = MaterialTheme.typography.titleMedium,
								maxLines = 1
							)
							if (state.queueSize > 1) {
								Text(
									text = "${state.queueIndex + 1} / ${state.queueSize}",
									color = Color.White.copy(alpha = 0.75f),
									style = MaterialTheme.typography.labelLarge
								)
							}
						}
						if (!state.isLocked) {
							IconButton(onClick = viewModel::cycleOrientation) {
								Icon(
									Icons.Default.ScreenRotation,
									contentDescription = "Orientation",
									tint = Color.White
								)
							}
							IconButton(onClick = viewModel::toggleFavorite) {
								Icon(
									if (state.video?.isFavorite == true) Icons.Default.Favorite
									else Icons.Default.FavoriteBorder,
									null,
									tint = Color.White
								)
							}
							IconButton(onClick = {
								state.video?.id?.let(onOpenDetails)
							}) {
								Icon(Icons.Default.Info, null, tint = Color.White)
							}
						}
						IconButton(onClick = viewModel::toggleLock) {
							Icon(
								if (state.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
								null,
								tint = Color.White
							)
						}
					}
					if (!state.isLocked) {
						Text(
							text = when (state.orientation) {
								PlayerOrientation.AUTO -> "Orientation: Auto"
								PlayerOrientation.PORTRAIT -> "Orientation: Portrait"
								PlayerOrientation.LANDSCAPE -> "Orientation: Landscape"
							},
							color = Color.White.copy(alpha = 0.7f),
							style = MaterialTheme.typography.labelLarge,
							modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
						)
					}
				}

				if (!state.isLocked) {
					Row(
						modifier = Modifier.align(Alignment.Center),
						horizontalArrangement = Arrangement.spacedBy(20.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						IconButton(
							onClick = viewModel::playPrevious,
							modifier = Modifier
								.size(52.dp)
								.background(Color.Black.copy(0.35f), CircleShape)
						) {
							Icon(
								Icons.Default.SkipPrevious,
								contentDescription = "Previous",
								tint = Color.White,
								modifier = Modifier.size(30.dp)
							)
						}
						IconButton(
							onClick = { viewModel.seekBy(-viewModel.seekStepMs()) },
							modifier = Modifier
								.size(52.dp)
								.background(Color.Black.copy(0.35f), CircleShape)
						) {
							Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(28.dp))
						}
						IconButton(
							onClick = viewModel::togglePlayPause,
							modifier = Modifier
								.size(72.dp)
								.background(Color.Black.copy(0.45f), CircleShape)
						) {
							Icon(
								if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
								null,
								tint = Color.White,
								modifier = Modifier.size(40.dp)
							)
						}
						IconButton(
							onClick = { viewModel.seekBy(viewModel.seekStepMs()) },
							modifier = Modifier
								.size(52.dp)
								.background(Color.Black.copy(0.35f), CircleShape)
						) {
							Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(28.dp))
						}
						IconButton(
							onClick = viewModel::playNext,
							enabled = nextEnabled,
							modifier = Modifier
								.size(52.dp)
								.background(Color.Black.copy(0.35f), CircleShape)
						) {
							Icon(
								Icons.Default.SkipNext,
								contentDescription = "Next",
								tint = if (nextEnabled) Color.White else Color.White.copy(alpha = 0.35f),
								modifier = Modifier.size(30.dp)
							)
						}
					}

					Column(
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.background(
								Brush.verticalGradient(
									listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
								)
							)
							.padding(horizontal = 12.dp, vertical = 10.dp)
					) {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(
								Formatters.formatDuration(state.positionMs),
								color = Color.White,
								style = MaterialTheme.typography.labelLarge
							)
							Slider(
								value = if (state.durationMs > 0) {
									state.positionMs.toFloat() / state.durationMs
								} else 0f,
								onValueChange = {
									if (state.durationMs > 0) {
										viewModel.seekTo((it * state.durationMs).toLong())
									}
								},
								modifier = Modifier
									.weight(1f)
									.padding(horizontal = 8.dp),
								colors = SliderDefaults.colors(
									thumbColor = MaterialTheme.colorScheme.primary,
									activeTrackColor = MaterialTheme.colorScheme.primary
								)
							)
							Text(
								Formatters.formatDuration(state.durationMs),
								color = Color.White,
								style = MaterialTheme.typography.labelLarge
							)
						}
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically
						) {
							IconButton(onClick = viewModel::cycleSpeed) {
								Row(verticalAlignment = Alignment.CenterVertically) {
									Icon(Icons.Default.Speed, null, tint = Color.White)
									Spacer(modifier = Modifier.width(4.dp))
									Text(
										Formatters.formatSpeed(state.playbackSpeed),
										color = Color.White
									)
								}
							}
							IconButton(onClick = viewModel::cycleRepeatMode) {
								Icon(
									imageVector = when (state.repeatMode) {
										QueueRepeatMode.ONE -> Icons.Default.RepeatOne
										else -> Icons.Default.Repeat
									},
									contentDescription = "Repeat",
									tint = when (state.repeatMode) {
										QueueRepeatMode.OFF -> Color.White.copy(alpha = 0.45f)
										else -> MaterialTheme.colorScheme.primary
									}
								)
							}
							Box {
								IconButton(onClick = { subtitleMenu = true }) {
									Icon(Icons.Default.Subtitles, null, tint = Color.White)
								}
								DropdownMenu(
									expanded = subtitleMenu,
									onDismissRequest = { subtitleMenu = false }
								) {
									DropdownMenuItem(
										text = { Text("Off") },
										onClick = {
											viewModel.selectSubtitle(-1)
											subtitleMenu = false
										}
									)
									state.subtitleTracks.forEachIndexed { index, track ->
										DropdownMenuItem(
											text = { Text(track.label) },
											onClick = {
												viewModel.selectSubtitle(index)
												subtitleMenu = false
											}
										)
									}
									DropdownMenuItem(
										text = { Text("Load SRT / VTT / ASS…") },
										onClick = {
											subtitleMenu = false
											subtitlePicker.launch(arrayOf("*/*"))
										}
									)
								}
							}
							IconButton(onClick = {
								resizeMode = when (resizeMode) {
									AspectRatioFrameLayout.RESIZE_MODE_FIT ->
										AspectRatioFrameLayout.RESIZE_MODE_ZOOM
									AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
										AspectRatioFrameLayout.RESIZE_MODE_FILL
									else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
								}
							}) {
								Icon(Icons.Default.AspectRatio, null, tint = Color.White)
							}
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								IconButton(onClick = {
									enterPip(activity, state.video?.width ?: 16, state.video?.height ?: 9)
								}) {
									Icon(Icons.Default.PictureInPictureAlt, null, tint = Color.White)
								}
							}
						}
					}
				} else {
					Text(
						text = "Screen locked — tap lock to unlock",
						color = Color.White.copy(alpha = 0.8f),
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.padding(24.dp)
					)
				}
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
