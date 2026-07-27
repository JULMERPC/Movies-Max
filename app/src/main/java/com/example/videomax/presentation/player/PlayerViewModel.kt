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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

data class PlayerProgressState(
	val positionMs: Long = 0L,
	val bufferedMs: Long = 0L,
	val durationMs: Long = 0L
)

data class PlayerUiState(
	val video: Video? = null,
	val isPlaying: Boolean = false,
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
	private val settingsRepository: com.example.videomax.domain.repository.SettingsRepository,
	observeSettings: ObserveSettingsUseCase
) : AndroidViewModel(application) {

	val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
		playWhenReady = true
		repeatMode = Player.REPEAT_MODE_OFF
	}

	private val initialVideoId: Long = checkNotNull(savedStateHandle["videoId"])

	private val _uiState = MutableStateFlow(PlayerUiState())
	val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

	/** High-frequency timeline updates — collected only by the progress bar UI. */
	private val _progressState = MutableStateFlow(PlayerProgressState())
	val progressState: StateFlow<PlayerProgressState> = _progressState.asStateFlow()

	private var settings: AppSettings = AppSettings()
	private var progressJob: Job? = null
	private var hideControlsJob: Job? = null
	private var gestureHintJob: Job? = null
	private var externalSubtitles: List<SubtitleTrack> = emptyList()
	private var isSwitchingMedia = false
	private var playCountRecordedFor: Long? = null
	private val expandMutex = Mutex()
	/** Session cache: videoId → sidecar tracks (avoids re-scanning the same file). */
	private val subtitleCache = mutableMapOf<Long, List<SubtitleTrack>>()
	private var subtitleLoadJob: Job? = null
	/** Eagerly-saved position for onCleared — avoids runBlocking on the main thread. */
	@Volatile
	private var lastPersistedPositionMs: Long = 0L
	@Volatile
	private var lastPersistedVideoId: Long = 0L
	/** Non-cancellable scope for final cleanup work in onCleared. */
	private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	private val playerListener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			_uiState.update { it.copy(isPlaying = isPlaying) }
			if (isPlaying) scheduleHideControls() else cancelHideControls()
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_READY) {
				publishProgress(
					positionMs = player.currentPosition.coerceAtLeast(0L),
					bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
					durationMs = player.duration.coerceAtLeast(0L)
				)
			}
			if (playbackState == Player.STATE_ENDED) {
				viewModelScope.launch {
					persistProgress()
					when (_uiState.value.repeatMode) {
						QueueRepeatMode.ONE -> {
							player.seekTo(0L)
							player.play()
						}
					QueueRepeatMode.ALL -> {
						if (settings.autoPlayNext) playNextInternal()
						else _uiState.update { it.copy(isPlaying = false, controlsVisible = true) }
					}
						QueueRepeatMode.OFF -> {
							if (settings.autoPlayNext && (player.hasNextMediaItem() || playbackQueue.hasNext())) {
								playNextInternal()
							} else {
								_uiState.update { it.copy(isPlaying = false, controlsVisible = true) }
							}
						}
					}
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			val video = mediaItem?.localConfiguration?.tag as? Video ?: return

			viewModelScope.launch {
				persistProgress()
			}

			val newIndex = player.currentMediaItemIndex
			playbackQueue.setIndex(newIndex)

			val videoId = video.id
			if (playCountRecordedFor != videoId) {
				playCountRecordedFor = videoId
				viewModelScope.launch {
					runCatching { videoRepository.incrementPlayCount(videoId) }
					runCatching { videoRepository.markVideoSeen(videoId) }
				}
			}

			_uiState.update {
				it.copy(
					video = video,
					subtitleTracks = emptyList(),
					selectedSubtitleIndex = -1,
					controlsVisible = true
				)
			}
			publishProgress(
				positionMs = 0L,
				bufferedMs = 0L,
				durationMs = video.durationMs
			)
			externalSubtitles = emptyList()

			publishQueueState()
			showControls()
			scheduleSidecarSubtitleLoad(video)
			viewModelScope.launch { maybeExpandAroundPlayhead() }

			if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !settings.autoPlayNext) {
				player.pause()
				_uiState.update { it.copy(isPlaying = false, controlsVisible = true) }
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
			val currentId = playbackQueue.currentId() ?: initialVideoId
			setupPlaylist(currentId)
		}
		progressJob = viewModelScope.launch {
			while (isActive) {
				val position = player.currentPosition.coerceAtLeast(0L)
				val buffered = player.bufferedPosition.coerceAtLeast(0L)
				val duration = player.duration.takeIf { d -> d > 0 }
					?: _progressState.value.durationMs
				publishProgress(position, buffered, duration)
				delay(PROGRESS_POLL_MS)
			}
		}
	}

	/**
	 * Emits progress only when values actually change, so collectors skip no-op frames.
	 */
	private fun publishProgress(positionMs: Long, bufferedMs: Long, durationMs: Long) {
		val next = PlayerProgressState(
			positionMs = positionMs,
			bufferedMs = bufferedMs,
			durationMs = durationMs.coerceAtLeast(0L)
		)
		if (next != _progressState.value) {
			_progressState.value = next
		}
	}
	private fun publishQueueState() {
		_uiState.update {
			it.copy(
				queueIndex = playbackQueue.currentIndex(),
				queueSize = playbackQueue.size(),
				hasPrevious = playbackQueue.hasPrevious() ||
					(_uiState.value.repeatMode == QueueRepeatMode.ALL && playbackQueue.size() > 1),
				hasNext = playbackQueue.hasNext() ||
					it.repeatMode == QueueRepeatMode.ALL
			)
		}
	}

	/**
	 * Starts the selected video immediately, then builds / expands the ExoPlayer
	 * playlist without blocking first paint / first frame.
	 * Sidecar subtitles are loaded only for the playing item (never for the whole queue).
	 */
	private fun setupPlaylist(startVideoId: Long) {
		viewModelScope.launch {
			val startVideo = getVideoById(startVideoId)
				?: videoRepository.getVideoById(startVideoId)
				?: return@launch

			val resumePosition = if (settings.rememberPlaybackPosition && startVideo.lastPositionMs > 0) {
				startVideo.lastPositionMs
			} else {
				0L
			}

			isSwitchingMedia = true
			try {
				player.setMediaItems(listOf(buildMediaItem(startVideo)), 0, resumePosition)
				player.prepare()
				player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
				applyPlayerRepeatMode(_uiState.value.repeatMode)
				player.playWhenReady = true
			} finally {
				isSwitchingMedia = false
			}

			viewModelScope.launch {
				runCatching { videoRepository.markVideoSeen(startVideoId) }
			}

			_uiState.update {
				it.copy(
					video = startVideo,
					subtitleTracks = emptyList(),
					selectedSubtitleIndex = -1,
					controlsVisible = true
				)
			}
			publishProgress(
				positionMs = resumePosition,
				bufferedMs = 0L,
				durationMs = startVideo.durationMs
			)
			publishQueueState()
			showControls()

			// Non-blocking: discover sidecars for this video only while playback already runs.
			scheduleSidecarSubtitleLoad(startVideo)

			if (playbackQueue.isLazy()) {
				expandInitialLazyWindow(startVideoId)
			} else {
				loadEagerPlaylist(startVideoId, resumePosition)
			}
			// Playlist rebuilds strip MediaItem subtitle configs — re-attach from cache/IO.
			_uiState.value.video?.let { scheduleSidecarSubtitleLoad(it) }
		}
	}

	private fun scheduleSidecarSubtitleLoad(video: Video) {
		subtitleLoadJob?.cancel()
		subtitleLoadJob = viewModelScope.launch {
			loadAndAttachSidecarSubtitles(video)
		}
	}

	/**
	 * Loads external subtitles for [video] on [Dispatchers.IO] (with session cache),
	 * then attaches them to the current MediaItem without stopping playback.
	 * Uses [C.SELECTION_FLAG_DEFAULT] so ExoPlayer auto-selects when tracks exist.
	 */
	private suspend fun loadAndAttachSidecarSubtitles(video: Video) {
		val tracks = subtitleCache[video.id] ?: withContext(Dispatchers.IO) {
			runCatching { SubtitleHelper.findExternalSubtitles(video.path) }
				.getOrDefault(emptyList())
		}.also { subtitleCache[video.id] = it }

		// User may have already switched away.
		if (_uiState.value.video?.id != video.id) return
		if (player.currentMediaItem?.mediaId != video.id.toString()) return

		externalSubtitles = tracks
		_uiState.update { it.copy(subtitleTracks = tracks) }
		if (tracks.isEmpty()) return

		val configs = tracks.map { track ->
			MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.uri))
				.setMimeType(track.mimeType)
				.setLabel(track.label)
				.setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
				.build()
		}
		val position = player.currentPosition
		val playWhenReady = player.playWhenReady
		val index = player.currentMediaItemIndex
		if (index < 0) return

		isSwitchingMedia = true
		try {
			player.replaceMediaItem(
				index,
				MediaItem.Builder()
					.setUri(Uri.parse(video.uri))
					.setMediaId(video.id.toString())
					.setSubtitleConfigurations(configs)
					.setTag(video)
					.build()
			)
			player.seekTo(index, position)
			player.playWhenReady = playWhenReady
			// Keep text tracks enabled so DEFAULT selection can apply (auto subtitle).
			player.trackSelectionParameters = player.trackSelectionParameters
				.buildUpon()
				.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
				.build()
		} finally {
			isSwitchingMedia = false
		}
	}

	private suspend fun loadEagerPlaylist(startVideoId: Long, resumePosition: Long) {
		val ids = playbackQueue.ids()
		if (ids.size <= 1) {
			publishQueueState()
			return
		}
		val mediaItems = buildMediaItems(ids)
		if (mediaItems.isEmpty()) return
		val startIndex = mediaItems.indexOfFirst { it.mediaId == startVideoId.toString() }.coerceAtLeast(0)
		val playWhenReady = player.playWhenReady
		val position = player.currentPosition.takeIf { it > 0 } ?: resumePosition

		isSwitchingMedia = true
		try {
			player.setMediaItems(mediaItems, startIndex, position)
			player.prepare()
			player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
			applyPlayerRepeatMode(_uiState.value.repeatMode)
			player.playWhenReady = playWhenReady
		} finally {
			isSwitchingMedia = false
		}
		publishQueueState()
	}

	private suspend fun expandInitialLazyWindow(startVideoId: Long) {
		expandMutex.withLock {
			val ctx = playbackQueue.context() ?: return
			val window = videoRepository.getQueueWindow(
				query = ctx.query,
				sortOption = ctx.sortOption,
				folder = ctx.folder,
				anchorId = startVideoId,
				before = PlaybackQueue.WINDOW_BEFORE,
				after = PlaybackQueue.WINDOW_AFTER
			)
			playbackQueue.applyWindow(
				windowIds = window.ids,
				startId = startVideoId,
				moreBefore = window.hasMoreBefore,
				moreAfter = window.hasMoreAfter
			)

			val mediaItems = buildMediaItems(window.ids)
			if (mediaItems.isEmpty()) return
			val startIndex = mediaItems.indexOfFirst { it.mediaId == startVideoId.toString() }.coerceAtLeast(0)
			val position = player.currentPosition
			val playWhenReady = player.playWhenReady

			isSwitchingMedia = true
			try {
				player.setMediaItems(mediaItems, startIndex, position)
				player.prepare()
				player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
				applyPlayerRepeatMode(_uiState.value.repeatMode)
				player.playWhenReady = playWhenReady
			} finally {
				isSwitchingMedia = false
			}
			publishQueueState()
		}
	}

	private suspend fun maybeExpandAroundPlayhead() {
		if (!playbackQueue.isLazy()) return
		if (playbackQueue.needsExpandAfter()) expandAfterLocked()
		if (playbackQueue.needsExpandBefore()) expandBeforeLocked()
	}

	private suspend fun expandAfterLocked() = expandMutex.withLock {
		if (!playbackQueue.isLazy() || !playbackQueue.hasMoreAfter()) return
		val ctx = playbackQueue.context() ?: return
		val anchor = playbackQueue.lastId() ?: return
		val more = videoRepository.getQueueNeighborsAfter(
			query = ctx.query,
			sortOption = ctx.sortOption,
			folder = ctx.folder,
			anchorId = anchor,
			limit = PlaybackQueue.EXPAND_CHUNK
		)
		if (more.isEmpty()) {
			playbackQueue.append(emptyList(), moreAfter = false)
			publishQueueState()
			return
		}
		val added = playbackQueue.append(more, moreAfter = more.size >= PlaybackQueue.EXPAND_CHUNK)
		if (added > 0) {
			val newIds = playbackQueue.ids().takeLast(added)
			player.addMediaItems(buildMediaItems(newIds))
		}
		publishQueueState()
	}

	private suspend fun expandBeforeLocked() = expandMutex.withLock {
		if (!playbackQueue.isLazy() || !playbackQueue.hasMoreBefore()) return
		val ctx = playbackQueue.context() ?: return
		val anchor = playbackQueue.firstId() ?: return
		val more = videoRepository.getQueueNeighborsBefore(
			query = ctx.query,
			sortOption = ctx.sortOption,
			folder = ctx.folder,
			anchorId = anchor,
			limit = PlaybackQueue.EXPAND_CHUNK
		)
		if (more.isEmpty()) {
			playbackQueue.prepend(emptyList(), moreBefore = false)
			publishQueueState()
			return
		}
		val added = playbackQueue.prepend(more, moreBefore = more.size >= PlaybackQueue.EXPAND_CHUNK)
		if (added > 0) {
			val newIds = playbackQueue.ids().take(added)
			player.addMediaItems(0, buildMediaItems(newIds))
		}
		publishQueueState()
	}

	/** Wrap Repeat All to the first page of the filtered library. */
	private suspend fun wrapToLibraryStart() {
		expandMutex.withLock {
			val ctx = playbackQueue.context() ?: return
			val limit = PlaybackQueue.WINDOW_BEFORE + PlaybackQueue.WINDOW_AFTER + 1
			val firstIds = videoRepository.getQueueFirstIds(
				query = ctx.query,
				sortOption = ctx.sortOption,
				folder = ctx.folder,
				limit = limit
			)
			if (firstIds.isEmpty()) return
			val startId = firstIds.first()
			playbackQueue.applyWindow(
				windowIds = firstIds,
				startId = startId,
				moreBefore = false,
				moreAfter = firstIds.size >= limit
			)
			val mediaItems = buildMediaItems(firstIds)
			isSwitchingMedia = true
			try {
				player.setMediaItems(mediaItems, 0, C.TIME_UNSET)
				player.prepare()
				player.playWhenReady = true
			} finally {
				isSwitchingMedia = false
			}
			publishQueueState()
		}
		// Re-apply sidecars after setMediaItems wiped configs.
		_uiState.value.video?.let { scheduleSidecarSubtitleLoad(it) }
	}

	/** Wrap Repeat All previous to the last page of the filtered library. */
	private suspend fun wrapToLibraryEnd() {
		expandMutex.withLock {
			val ctx = playbackQueue.context() ?: return
			val limit = PlaybackQueue.WINDOW_BEFORE + PlaybackQueue.WINDOW_AFTER + 1
			val lastIds = videoRepository.getQueueLastIds(
				query = ctx.query,
				sortOption = ctx.sortOption,
				folder = ctx.folder,
				limit = limit
			)
			if (lastIds.isEmpty()) return
			val startId = lastIds.last()
			playbackQueue.applyWindow(
				windowIds = lastIds,
				startId = startId,
				moreBefore = lastIds.size >= limit,
				moreAfter = false
			)
			val mediaItems = buildMediaItems(lastIds)
			val startIndex = mediaItems.lastIndex.coerceAtLeast(0)
			isSwitchingMedia = true
			try {
				player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
				player.prepare()
				player.playWhenReady = true
			} finally {
				isSwitchingMedia = false
			}
			publishQueueState()
		}
		_uiState.value.video?.let { scheduleSidecarSubtitleLoad(it) }
	}

	private suspend fun buildMediaItems(ids: List<Long>): List<MediaItem> =
		withContext(Dispatchers.IO) {
			if (ids.isEmpty()) return@withContext emptyList()
			val videos = videoRepository.getVideosByIds(ids)
			val map = videos.associateBy { it.id }
			// Never scan the filesystem for subtitles while building the queue.
			ids.mapNotNull { id -> map[id]?.let { buildMediaItem(it) } }
		}

	/** MediaItem without sidecar configs — subs are attached lazily for the playing item. */
	private fun buildMediaItem(video: Video): MediaItem =
		MediaItem.Builder()
			.setUri(Uri.parse(video.uri))
			.setMediaId(video.id.toString())
			.setTag(video)
			.build()


	fun playPrevious() {
		viewModelScope.launch {
			val position = player.currentPosition
			if (position > 3_000L) {
				player.seekTo(0L)
				showControls()
				return@launch
			}
			if (!player.hasPreviousMediaItem() && playbackQueue.hasMoreBefore()) {
				expandBeforeLocked()
			}
			when {
				player.hasPreviousMediaItem() -> player.seekToPreviousMediaItem()
				_uiState.value.repeatMode == QueueRepeatMode.ALL -> {
					if (playbackQueue.isLazy()) {
						wrapToLibraryEnd()
					} else if (player.mediaItemCount > 0) {
						player.seekToDefaultPosition(player.mediaItemCount - 1)
						player.play()
					} else {
						player.seekTo(0L)
					}
				}
				else -> player.seekTo(0L)
			}
			showControls()
			maybeExpandAroundPlayhead()
		}
	}

	fun playNext() {
		viewModelScope.launch { playNextInternal() }
	}

	private suspend fun playNextInternal() {
		if (!player.hasNextMediaItem() && playbackQueue.hasMoreAfter()) {
			expandAfterLocked()
		}
		when {
			player.hasNextMediaItem() -> player.seekToNextMediaItem()
			_uiState.value.repeatMode == QueueRepeatMode.ALL -> {
				if (playbackQueue.isLazy()) {
					wrapToLibraryStart()
				} else {
					player.seekToDefaultPosition(0)
					player.play()
				}
			}
			_uiState.value.repeatMode == QueueRepeatMode.ONE -> {
				player.seekTo(0L)
				player.play()
			}
			else -> {
				player.seekTo(0L)
			}
		}
		showControls()
		maybeExpandAroundPlayhead()
	}

	fun cycleRepeatMode() {
		val modes = QueueRepeatMode.entries
		val current = _uiState.value.repeatMode
		val next = modes[(modes.indexOf(current) + 1) % modes.size]
		_uiState.update { it.copy(repeatMode = next) }
		applyPlayerRepeatMode(next)
		publishQueueState()
		showControls()
	}

	/**
	 * For lazy queues, ExoPlayer ALL would only loop the loaded window — handle wrap ourselves.
	 * ONE still uses ExoPlayer's native repeat.
	 */
	private fun applyPlayerRepeatMode(mode: QueueRepeatMode) {
		player.repeatMode = when {
			mode == QueueRepeatMode.ONE -> Player.REPEAT_MODE_ONE
			playbackQueue.isLazy() -> Player.REPEAT_MODE_OFF
			mode == QueueRepeatMode.ALL && settings.autoPlayNext -> Player.REPEAT_MODE_ALL
			else -> Player.REPEAT_MODE_OFF
		}
	}

	fun cycleOrientation(isDeviceLandscape: Boolean) {
		val next = when (_uiState.value.orientation) {
			PlayerOrientation.AUTO -> if (isDeviceLandscape) PlayerOrientation.LANDSCAPE else PlayerOrientation.PORTRAIT
			else -> PlayerOrientation.AUTO
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
		val video = _uiState.value.video
		if (video != null) {
			subtitleCache[video.id] = externalSubtitles
		}
		_uiState.update { it.copy(subtitleTracks = externalSubtitles) }
		if (video == null) return
		val configs = externalSubtitles.map {
			MediaItem.SubtitleConfiguration.Builder(Uri.parse(it.uri))
				.setMimeType(it.mimeType)
				.setLabel(it.label)
				.setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
				.build()
		}
		val playWhenReady = player.playWhenReady
		val speed = _uiState.value.playbackSpeed
		isSwitchingMedia = true

		val newMediaItem = MediaItem.Builder()
			.setUri(Uri.parse(video.uri))
			.setMediaId(video.id.toString())
			.setSubtitleConfigurations(configs)
			.setTag(video)
			.build()

		val currentIndex = player.currentMediaItemIndex
		player.replaceMediaItem(currentIndex, newMediaItem)
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

	fun toggleAutoPip() {
		val next = !_uiState.value.autoPip
		viewModelScope.launch {
			settingsRepository.setAutoPip(next)
		}
		_uiState.update { it.copy(autoPip = next) }
	}

	fun seekStepMs(): Long = settings.seekStepSeconds * 1000L

	/**
	 * Pauses playback and persists the current position.
	 * Called when the player screen goes to background (ON_STOP) or is being destroyed.
	 */
	fun pauseAndPersist() {
		if (player.isPlaying) {
			player.pause()
		}
		viewModelScope.launch {
			runCatching { persistProgress() }
		}
	}

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
		val effectivePosition = if (position > duration - 3_000) 0L else position
		// Cache for onCleared safety.
		lastPersistedVideoId = video.id
		lastPersistedPositionMs = effectivePosition
		saveProgress(
			videoId = video.id,
			videoUri = video.uri,
			displayName = video.displayName,
			positionMs = effectivePosition,
			durationMs = duration
		)
	}

	override fun onCleared() {
		// Persist position without blocking the main thread.
		val videoId = lastPersistedVideoId
		val position = lastPersistedPositionMs
		if (videoId > 0) {
			cleanupScope.launch {
				runCatching {
					val video = _uiState.value.video
					if (video != null && video.id == videoId) {
						val duration = player.duration.coerceAtLeast(0L)
						if (duration > 0L) {
							saveProgress(
								videoId = video.id,
								videoUri = video.uri,
								displayName = video.displayName,
								positionMs = position,
								durationMs = duration
							)
						}
					}
				}
			}
		}
		subtitleLoadJob?.cancel()
		subtitleCache.clear()
		progressJob?.cancel()
		hideControlsJob?.cancel()
		gestureHintJob?.cancel()
		player.removeListener(playerListener)
		player.stop()
		player.clearMediaItems()
		player.release()
		cleanupScope.cancel()
		super.onCleared()
	}

	fun getQueueIds(): List<Long> = playbackQueue.ids()

	fun getCurrentQueueIndex(): Int = playbackQueue.currentIndex()

	fun getCurrentVideoId(): Long? = playbackQueue.currentId()

	suspend fun getQueueVideos(): List<Video> {
		val ids = playbackQueue.ids()
		if (ids.isEmpty()) return emptyList()
		return videoRepository.getVideosByIds(ids)
	}

	fun playQueueItem(index: Int) {
		val ids = playbackQueue.ids()
		if (index !in ids.indices) return
		val videoId = ids[index]
		viewModelScope.launch {
			val video = videoRepository.getVideoById(videoId) ?: return@launch
			persistProgress()

			val isNearby = kotlin.math.abs(index - playbackQueue.currentIndex()) <= 1
			if (isNearby && index == playbackQueue.currentIndex()) {
				player.seekTo(0L)
				player.play()
				showControls()
				return@launch
			}

			if (player.currentMediaItemIndex in 0 until player.mediaItemCount &&
				player.currentMediaItemIndex != index
			) {
				player.seekToDefaultPosition(index)
				player.play()
				showControls()
				return@launch
			}

			val allIds = playbackQueue.ids()
			val mediaItems = buildMediaItems(allIds)
			if (mediaItems.isEmpty()) return@launch
			val startIdx = index.coerceIn(0, mediaItems.lastIndex)
			val playWhenReady = player.playWhenReady

			isSwitchingMedia = true
			try {
				player.setMediaItems(mediaItems, startIdx, 0L)
				player.prepare()
				player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
				applyPlayerRepeatMode(_uiState.value.repeatMode)
				player.playWhenReady = playWhenReady
			} finally {
				isSwitchingMedia = false
			}
			publishQueueState()
			showControls()
		}
	}

	suspend fun shuffleQueue() {
		val ids = playbackQueue.ids().toMutableList()
		if (ids.size < 2) return
		val currentVideoId = playbackQueue.currentId() ?: return
		val rest = ids.filter { it != currentVideoId }.shuffled()
		val shuffled = mutableListOf(currentVideoId)
		shuffled.addAll(rest)
		val mediaItems = buildMediaItems(shuffled)
		if (mediaItems.isEmpty()) return
		val playWhenReady = player.playWhenReady
		val currentPos = player.currentPosition

		isSwitchingMedia = true
		try {
			player.setMediaItems(mediaItems, 0, currentPos)
			player.prepare()
			player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
			applyPlayerRepeatMode(_uiState.value.repeatMode)
			player.playWhenReady = playWhenReady
		} finally {
			isSwitchingMedia = false
		}
		playbackQueue.setQueue(shuffled, currentVideoId)
		publishQueueState()
	}

	private companion object {
		const val PROGRESS_POLL_MS = 500L
	}
}
