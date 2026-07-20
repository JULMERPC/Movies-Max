package com.example.videomax.presentation.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.SubtitleTrack
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.usecase.GetVideoByIdUseCase
import com.example.videomax.domain.usecase.ObserveSettingsUseCase
import com.example.videomax.domain.usecase.SavePlaybackProgressUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.util.SubtitleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.abs

data class PlayerUiState(
	val video: Video? = null,
	val isPlaying: Boolean = false,
	val positionMs: Long = 0L,
	val durationMs: Long = 0L,
	val bufferedMs: Long = 0L,
	val playbackSpeed: Float = 1f,
	val controlsVisible: Boolean = true,
	val isLocked: Boolean = false,
	val isFullscreen: Boolean = true,
	val subtitleTracks: List<SubtitleTrack> = emptyList(),
	val selectedSubtitleIndex: Int = -1,
	val gestureHint: String? = null,
	val brightness: Float = 0.5f,
	val volumeFraction: Float = 0.5f,
	val brightnessOverrideEnabled: Boolean = false,
	val gesturesEnabled: Boolean = true,
	val autoPip: Boolean = false,
	val orientation: PlayerOrientation = PlayerOrientation.AUTO,
	val repeatMode: QueueRepeatMode = QueueRepeatMode.OFF,
	val queueIndex: Int = 0,
	val queueSize: Int = 1,
	val hasPrevious: Boolean = false,
	val hasNext: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
	application: Application,
	savedStateHandle: SavedStateHandle,
	private val getVideoById: GetVideoByIdUseCase,
	private val saveProgress: SavePlaybackProgressUseCase,
	private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
	private val playbackQueue: PlaybackQueue,
	private val videoRepository: VideoRepository,
	observeSettings: ObserveSettingsUseCase
) : AndroidViewModel(application) {

	private val initialVideoId: Long = checkNotNull(savedStateHandle["videoId"])

	val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
		playWhenReady = true
		repeatMode = Player.REPEAT_MODE_OFF
	}

	private val _uiState = MutableStateFlow(PlayerUiState())
	val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

	private var settings: AppSettings = AppSettings()
	private var progressJob: Job? = null
	private var hideControlsJob: Job? = null
	private var gestureHintJob: Job? = null
	private var externalSubtitles: List<SubtitleTrack> = emptyList()
	private var isSwitchingMedia = false
	private var playCountRecordedFor: Long? = null

	private val playerListener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			_uiState.update { it.copy(isPlaying = isPlaying) }
			if (isPlaying) scheduleHideControls() else cancelHideControls()
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_READY) {
				_uiState.update {
					it.copy(durationMs = player.duration.coerceAtLeast(0L))
				}
			}
			if (playbackState == Player.STATE_ENDED && !isSwitchingMedia) {
				onVideoEnded()
			}
		}

		override fun onTracksChanged(tracks: Tracks) {
			// External subtitle list remains the UI source of truth
		}
	}

	init {
		playbackQueue.ensureSingle(initialVideoId)
		player.addListener(playerListener)
		viewModelScope.launch {
			observeSettings().collect { latest ->
				settings = latest
				_uiState.update {
					it.copy(
						gesturesEnabled = latest.gesturesEnabled,
						autoPip = latest.autoPip,
						playbackSpeed = if (it.video == null) latest.defaultPlaybackSpeed else it.playbackSpeed
					)
				}
			}
		}
		viewModelScope.launch {
			loadVideo(
				videoId = playbackQueue.currentId() ?: initialVideoId,
				resumePosition = true
			)
		}
		progressJob = viewModelScope.launch {
			while (isActive) {
				_uiState.update {
					it.copy(
						positionMs = player.currentPosition.coerceAtLeast(0L),
						bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
						durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs
					)
				}
				delay(500)
			}
		}
	}

	private fun publishQueueState() {
		_uiState.update {
			it.copy(
				queueIndex = playbackQueue.currentIndex(),
				queueSize = playbackQueue.size(),
				hasPrevious = playbackQueue.hasPrevious(),
				hasNext = playbackQueue.hasNext() || it.repeatMode == QueueRepeatMode.ALL
			)
		}
	}

	private suspend fun loadVideo(videoId: Long, resumePosition: Boolean) {
		val video = getVideoById(videoId) ?: return
		val keepSpeed = _uiState.value.playbackSpeed.takeIf { it > 0f }
			?: settings.defaultPlaybackSpeed
		val keepOrientation = _uiState.value.orientation
		val keepRepeat = _uiState.value.repeatMode
		val keepBrightness = _uiState.value.brightness
		val keepVolume = _uiState.value.volumeFraction
		val keepLocked = _uiState.value.isLocked
		val keepBrightnessOverride = _uiState.value.brightnessOverrideEnabled

		externalSubtitles = SubtitleHelper.findExternalSubtitles(video.path)
		_uiState.update {
			it.copy(
				video = video,
				subtitleTracks = externalSubtitles,
				selectedSubtitleIndex = -1,
				playbackSpeed = keepSpeed,
				orientation = keepOrientation,
				repeatMode = keepRepeat,
				brightness = keepBrightness,
				volumeFraction = keepVolume,
				brightnessOverrideEnabled = keepBrightnessOverride,
				isLocked = keepLocked,
				positionMs = 0L,
				durationMs = video.durationMs,
				controlsVisible = true
			)
		}
		publishQueueState()

		if (playCountRecordedFor != videoId) {
			playCountRecordedFor = videoId
			runCatching { videoRepository.incrementPlayCount(videoId) }
		}

		val subtitleConfigs = externalSubtitles.map { track ->
			MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.uri))
				.setMimeType(track.mimeType)
				.setLabel(track.label)
				.setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
				.build()
		}

		val mediaItem = MediaItem.Builder()
			.setUri(Uri.parse(video.uri))
			.setMediaId(video.id.toString())
			.setSubtitleConfigurations(subtitleConfigs)
			.build()

		isSwitchingMedia = true
		try {
			player.setMediaItem(mediaItem)
			player.prepare()
			player.playbackParameters = PlaybackParameters(keepSpeed)
			player.playWhenReady = true

			if (resumePosition && settings.rememberPlaybackPosition && video.lastPositionMs > 0) {
				player.seekTo(video.lastPositionMs)
			} else {
				player.seekTo(0L)
			}
		} finally {
			isSwitchingMedia = false
		}
		showControls()
	}

	private fun onVideoEnded() {
		viewModelScope.launch {
			persistProgress()
			when (_uiState.value.repeatMode) {
				QueueRepeatMode.ONE -> {
					player.seekTo(0L)
					player.play()
				}
				QueueRepeatMode.ALL -> {
					val nextId = if (playbackQueue.hasNext()) {
						playbackQueue.moveToNext()
					} else {
						playbackQueue.moveToFirst()
					}
					if (nextId != null) loadVideo(nextId, resumePosition = false)
				}
				QueueRepeatMode.OFF -> {
					if (playbackQueue.hasNext()) {
						playNextInternal(resumePosition = false)
					} else {
						player.pause()
						player.seekTo(0L)
						_uiState.update { it.copy(isPlaying = false, controlsVisible = true) }
					}
				}
			}
		}
	}

	fun playPrevious() {
		viewModelScope.launch {
			val position = player.currentPosition
			if (position > 3_000L) {
				player.seekTo(0L)
				showControls()
				return@launch
			}
			if (!playbackQueue.hasPrevious()) {
				player.seekTo(0L)
				showControls()
				return@launch
			}
			persistProgress()
			val previousId = playbackQueue.moveToPrevious() ?: return@launch
			loadVideo(previousId, resumePosition = false)
		}
	}

	fun playNext() {
		viewModelScope.launch {
			persistProgress()
			val state = _uiState.value
			when {
				playbackQueue.hasNext() -> playNextInternal(resumePosition = false)
				state.repeatMode == QueueRepeatMode.ALL -> {
					val firstId = playbackQueue.moveToFirst() ?: return@launch
					loadVideo(firstId, resumePosition = false)
				}
				state.repeatMode == QueueRepeatMode.ONE -> {
					player.seekTo(0L)
					player.play()
					showControls()
				}
				else -> {
					// Last item, no wrap — restart current
					player.seekTo(0L)
					showControls()
				}
			}
		}
	}

	private suspend fun playNextInternal(resumePosition: Boolean) {
		val nextId = playbackQueue.moveToNext() ?: return
		loadVideo(nextId, resumePosition = resumePosition)
	}

	fun cycleRepeatMode() {
		val modes = QueueRepeatMode.entries
		val current = _uiState.value.repeatMode
		val next = modes[(modes.indexOf(current) + 1) % modes.size]
		_uiState.update { it.copy(repeatMode = next) }
		publishQueueState()
		showControls()
	}

	fun cycleOrientation() {
		val next = when (_uiState.value.orientation) {
			PlayerOrientation.AUTO -> PlayerOrientation.LANDSCAPE
			PlayerOrientation.LANDSCAPE -> PlayerOrientation.PORTRAIT
			PlayerOrientation.PORTRAIT -> PlayerOrientation.AUTO
		}
		_uiState.update { it.copy(orientation = next) }
		showControls()
	}

	fun togglePlayPause() {
		if (player.isPlaying) player.pause() else player.play()
		showControls()
	}

	fun seekBy(deltaMs: Long) {
		val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
		player.seekTo(target)
		showControls()
	}

	fun seekTo(positionMs: Long) {
		player.seekTo(positionMs.coerceAtLeast(0L))
		showControls()
	}

	fun setSpeed(speed: Float) {
		player.playbackParameters = PlaybackParameters(speed)
		_uiState.update { it.copy(playbackSpeed = speed) }
		showControls()
	}

	fun cycleSpeed() {
		val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
		val current = _uiState.value.playbackSpeed
		val index = speeds.indexOfFirst { abs(it - current) < 0.01f }.takeIf { it >= 0 } ?: 2
		setSpeed(speeds[(index + 1) % speeds.size])
	}

	fun toggleLock() {
		_uiState.update { it.copy(isLocked = !it.isLocked, controlsVisible = true) }
		if (!_uiState.value.isLocked) scheduleHideControls()
	}

	fun toggleControls() {
		if (_uiState.value.isLocked) {
			_uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
			return
		}
		_uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
		if (_uiState.value.controlsVisible) scheduleHideControls()
	}

	fun showControls() {
		_uiState.update { it.copy(controlsVisible = true) }
		scheduleHideControls()
	}

	fun syncBrightnessWithoutHint(value: Float) {
		_uiState.update {
			it.copy(brightness = value.coerceIn(0.01f, 1f))
		}
	}

	fun setBrightness(value: Float, fromGesture: Boolean = true) {
		val coerced = value.coerceIn(0.01f, 1f)
		_uiState.update {
			it.copy(
				brightness = coerced,
				brightnessOverrideEnabled = if (fromGesture) true else it.brightnessOverrideEnabled,
				gestureHint = if (fromGesture) "Brightness ${(coerced * 100).toInt()}%" else it.gestureHint
			)
		}
		if (fromGesture) scheduleClearGestureHint()
	}

	fun setVolumeFraction(value: Float, fromGesture: Boolean = true) {
		val coerced = value.coerceIn(0f, 1f)
		_uiState.update {
			it.copy(
				volumeFraction = coerced,
				gestureHint = if (fromGesture) "Volume ${(coerced * 100).toInt()}%" else it.gestureHint
			)
		}
		if (fromGesture) scheduleClearGestureHint()
	}

	fun clearGestureHint() {
		_uiState.update { it.copy(gestureHint = null) }
	}

	private fun scheduleClearGestureHint() {
		gestureHintJob?.cancel()
		gestureHintJob = viewModelScope.launch {
			delay(1_200)
			clearGestureHint()
		}
	}

	fun selectSubtitle(index: Int) {
		_uiState.update { it.copy(selectedSubtitleIndex = index) }
		val trackGroups = player.currentTracks.groups
		val textGroups = trackGroups.filter { it.type == C.TRACK_TYPE_TEXT }
		if (index < 0) {
			player.trackSelectionParameters = player.trackSelectionParameters
				.buildUpon()
				.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
				.build()
		} else if (index in textGroups.indices) {
			val group = textGroups[index]
			player.trackSelectionParameters = player.trackSelectionParameters
				.buildUpon()
				.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
				.setOverrideForType(
					TrackSelectionOverride(group.mediaTrackGroup, 0)
				)
				.build()
		}
		showControls()
	}

	fun addExternalSubtitle(uri: Uri, mimeType: String, label: String) {
		val track = SubtitleTrack(uri.toString(), label, mimeType)
		externalSubtitles = externalSubtitles + track
		_uiState.update { it.copy(subtitleTracks = externalSubtitles) }
		val video = _uiState.value.video ?: return
		val configs = externalSubtitles.map {
			MediaItem.SubtitleConfiguration.Builder(Uri.parse(it.uri))
				.setMimeType(it.mimeType)
				.setLabel(it.label)
				.build()
		}
		val position = player.currentPosition
		val playWhenReady = player.playWhenReady
		val speed = _uiState.value.playbackSpeed
		isSwitchingMedia = true
		player.setMediaItem(
			MediaItem.Builder()
				.setUri(Uri.parse(video.uri))
				.setMediaId(video.id.toString())
				.setSubtitleConfigurations(configs)
				.build(),
			position
		)
		player.prepare()
		player.playbackParameters = PlaybackParameters(speed)
		player.playWhenReady = playWhenReady
		isSwitchingMedia = false
		selectSubtitle(externalSubtitles.lastIndex)
	}

	fun toggleFavorite() {
		val id = _uiState.value.video?.id ?: return
		viewModelScope.launch {
			toggleFavoriteUseCase(id)
			_uiState.update { state ->
				state.copy(video = state.video?.copy(isFavorite = !(state.video.isFavorite)))
			}
		}
	}

	fun seekStepMs(): Long = settings.seekStepSeconds * 1000L

	private fun scheduleHideControls() {
		hideControlsJob?.cancel()
		hideControlsJob = viewModelScope.launch {
			delay(3_500)
			if (player.isPlaying && !_uiState.value.isLocked) {
				_uiState.update { it.copy(controlsVisible = false) }
			}
		}
	}

	private fun cancelHideControls() {
		hideControlsJob?.cancel()
	}

	suspend fun persistProgress() {
		val video = _uiState.value.video ?: return
		val position = player.currentPosition.coerceAtLeast(0L)
		val duration = player.duration.coerceAtLeast(0L)
		if (duration <= 0L) return
		saveProgress(
			videoId = video.id,
			videoUri = video.uri,
			displayName = video.displayName,
			positionMs = if (position > duration - 3_000) 0L else position,
			durationMs = duration
		)
	}

	override fun onCleared() {
		runBlocking {
			runCatching { persistProgress() }
		}
		progressJob?.cancel()
		hideControlsJob?.cancel()
		gestureHintJob?.cancel()
		player.removeListener(playerListener)
		player.release()
		super.onCleared()
	}
}
