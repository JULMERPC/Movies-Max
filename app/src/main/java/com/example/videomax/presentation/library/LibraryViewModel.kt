package com.example.videomax.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.domain.usecase.ObserveVideosUseCase
import com.example.videomax.domain.usecase.ScanVideosUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.presentation.player.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
	val videos: List<Video> = emptyList(),
	val folders: List<String> = emptyList(),
	val query: String = "",
	val sortOption: SortOption = SortOption.DATE_DESC,
	val isScanning: Boolean = false,
	val isGrid: Boolean = true,
	val selectedFolder: String? = null,
	val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
	private val observeVideos: ObserveVideosUseCase,
	private val scanVideos: ScanVideosUseCase,
	private val toggleFavorite: ToggleFavoriteUseCase,
	private val videoRepository: VideoRepository,
	private val settingsRepository: SettingsRepository,
	private val playlistRepository: PlaylistRepository,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {

	private val query = MutableStateFlow("")
	private val sortOption = MutableStateFlow(SortOption.DATE_DESC)
	private val isScanning = MutableStateFlow(false)
	private val isGrid = MutableStateFlow(true)
	private val selectedFolder = MutableStateFlow<String?>(null)
	private val message = MutableStateFlow<String?>(null)

	private val videosFlow = combine(query, sortOption, selectedFolder) { q, sort, folder ->
		Triple(q, sort, folder)
	}.flatMapLatest { (q, sort, folder) ->
		if (folder == null) observeVideos(q, sort)
		else videoRepository.observeVideosByFolder(folder, sort)
	}

	private val controls = combine(query, sortOption, isScanning, isGrid, selectedFolder) {
			q, sort, scanning, grid, folder ->
		LibraryControls(q, sort, scanning, grid, folder)
	}

	val uiState: StateFlow<LibraryUiState> = combine(
		videosFlow,
		videoRepository.observeFolders(),
		controls,
		message
	) { videos, folders, ctrl, msg ->
		LibraryUiState(
			videos = videos,
			folders = folders,
			query = ctrl.query,
			sortOption = ctrl.sortOption,
			isScanning = ctrl.isScanning,
			isGrid = ctrl.isGrid,
			selectedFolder = ctrl.selectedFolder,
			message = msg
		)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

	init {
		viewModelScope.launch {
			settingsRepository.settings.collect { settings ->
				sortOption.value = settings.sortOption
			}
		}
		refresh()
	}

	fun onQueryChange(value: String) {
		query.value = value
	}

	fun onSortSelected(option: SortOption) {
		sortOption.value = option
		viewModelScope.launch { settingsRepository.setSortOption(option) }
	}

	fun toggleLayout() {
		isGrid.update { !it }
	}

	fun selectFolder(folder: String?) {
		selectedFolder.value = folder
	}

	fun refresh() {
		viewModelScope.launch {
			isScanning.value = true
			runCatching { scanVideos() }
				.onSuccess { count -> message.value = "Indexed $count videos" }
				.onFailure { message.value = it.message ?: "Scan failed" }
			isScanning.value = false
		}
	}

	fun clearMessage() {
		message.value = null
	}

	fun onFavorite(videoId: Long) {
		viewModelScope.launch { toggleFavorite(videoId) }
	}

	fun createPlaylist(name: String, videoId: Long? = null) {
		viewModelScope.launch {
			val id = playlistRepository.createPlaylist(name)
			if (videoId != null) playlistRepository.addVideoToPlaylist(id, videoId)
			message.value = "Playlist created"
		}
	}

	fun addToPlaylist(playlistId: Long, videoId: Long) {
		viewModelScope.launch {
			playlistRepository.addVideoToPlaylist(playlistId, videoId)
			message.value = "Added to playlist"
		}
	}

	fun preparePlayback(videoId: Long) {
		val ids = uiState.value.videos.map { it.id }
		playbackQueue.setQueue(ids, videoId)
	}

	private data class LibraryControls(
		val query: String,
		val sortOption: SortOption,
		val isScanning: Boolean,
		val isGrid: Boolean,
		val selectedFolder: String?
	)
}
