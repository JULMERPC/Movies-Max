package com.example.videomax.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.domain.usecase.PagingVideosUseCase
import com.example.videomax.domain.usecase.ScanVideosUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.presentation.player.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
	val folders: List<String> = emptyList(),
	val videoCount: Int = 0,
	val query: String = "",
	val sortOption: SortOption = SortOption.DATE_DESC,
	val isScanning: Boolean = false,
	val scanProgress: Float = 0f,
	val isGrid: Boolean = true,
	val selectedFolder: String? = null,
	val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
	private val pagingVideos: PagingVideosUseCase,
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
	private val scanProgress = MutableStateFlow(0f)
	private val isGrid = MutableStateFlow(true)
	private val selectedFolder = MutableStateFlow<String?>(null)
	private val message = MutableStateFlow<String?>(null)

	val videos: Flow<PagingData<Video>> = combine(query, sortOption, selectedFolder) { q, sort, folder ->
		Triple(q, sort, folder)
	}.flatMapLatest { (q, sort, folder) ->
		pagingVideos(q, sort, folder)
	}.cachedIn(viewModelScope)

	private val layoutControls = combine(isScanning, scanProgress, isGrid, selectedFolder) {
			scanning, progress, grid, folder ->
		LayoutControls(scanning, progress, grid, folder)
	}

	private val controls = combine(query, sortOption, layoutControls) { q, sort, layout ->
		LibraryControls(
			query = q,
			sortOption = sort,
			isScanning = layout.isScanning,
			scanProgress = layout.scanProgress,
			isGrid = layout.isGrid,
			selectedFolder = layout.selectedFolder
		)
	}


	val uiState: StateFlow<LibraryUiState> = combine(
		videoRepository.observeFolders(),
		videoRepository.observeVideoCount(),
		controls,
		message
	) { folders, count, ctrl, msg ->
		LibraryUiState(
			folders = folders,
			videoCount = count,
			query = ctrl.query,
			sortOption = ctrl.sortOption,
			isScanning = ctrl.isScanning,
			scanProgress = ctrl.scanProgress,
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
			scanProgress.value = 0f
			runCatching {
				scanVideos { indexed, total ->
					scanProgress.value = if (total > 0) indexed.toFloat() / total else 0f
				}
			}
				.onSuccess { count -> message.value = "Indexed $count videos" }
				.onFailure { message.value = it.message ?: "Scan failed" }
			isScanning.value = false
			scanProgress.value = 1f
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

	suspend fun preparePlayback(videoId: Long) {
		val ids = videoRepository.getVideoIds(
			query = query.value,
			sortOption = sortOption.value,
			folder = selectedFolder.value
		)
		playbackQueue.setQueue(ids, videoId)
	}

	private data class LayoutControls(
		val isScanning: Boolean,
		val scanProgress: Float,
		val isGrid: Boolean,
		val selectedFolder: String?
	)

	private data class LibraryControls(
		val query: String,
		val sortOption: SortOption,
		val isScanning: Boolean,
		val scanProgress: Float,
		val isGrid: Boolean,
		val selectedFolder: String?
	)
}
