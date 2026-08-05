package com.puma.videomax.presentation.library

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.puma.videomax.domain.model.SortOption
import com.puma.videomax.domain.model.Video
import com.puma.videomax.domain.repository.PlaylistRepository
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.repository.VideoRepository
import com.puma.videomax.domain.usecase.PagingVideosUseCase
import com.puma.videomax.domain.usecase.ScanVideosUseCase
import com.puma.videomax.domain.usecase.ToggleFavoriteUseCase
import com.puma.videomax.presentation.player.PlaybackQueue
import com.puma.videomax.presentation.player.PlaybackQueueContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LibraryFilterMode {
	ALL_VIDEOS,
	ALL_FOLDERS,
	FOLDER_TREE
}

data class LibraryUiState(
	val folders: List<String> = emptyList(),
	val videoCount: Int = 0,
	val query: String = "",
	val sortOption: SortOption = SortOption.DATE_DESC,
	val isScanning: Boolean = false,
	val scanProgress: Float = 0f,
	val isGrid: Boolean = true,
	val selectedFolder: String? = null,
	val filterMode: LibraryFilterMode = LibraryFilterMode.ALL_VIDEOS,
	val isSearchOpen: Boolean = false,
	val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
	private val application: Application,
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
	private val filterMode = MutableStateFlow(LibraryFilterMode.ALL_VIDEOS)
	private val isSearchOpen = MutableStateFlow(false)
	private val message = MutableStateFlow<String?>(null)
	private val hasAutoScanned = MutableStateFlow(false)

	private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
		override fun onChange(selfChange: Boolean, uri: Uri?) {
			if (!isScanning.value && hasAutoScanned.value) {
				checkForNewVideos()
			}
		}
	}

	@OptIn(kotlinx.coroutines.FlowPreview::class)
	private val debouncedQuery = query
		.debounce(SEARCH_DEBOUNCE_MS)
		.distinctUntilChanged()

	val videos: Flow<PagingData<Video>> = combine(debouncedQuery, sortOption, selectedFolder) { q, sort, folder ->
		Triple(q, sort, folder)
	}.flatMapLatest { (q, sort, folder) ->
		pagingVideos(q, sort, folder)
	}.cachedIn(viewModelScope)

	private val layoutControls = combine(isScanning, scanProgress, isGrid, selectedFolder) {
			scanning, progress, grid, folder ->
		LayoutControls(scanning, progress, grid, folder)
	}

	private val controls = combine(query, sortOption, layoutControls, filterMode, isSearchOpen) {
			q, sort, layout, mode, searchOpen ->
		LibraryControls(
			query = q,
			sortOption = sort,
			isScanning = layout.isScanning,
			scanProgress = layout.scanProgress,
			isGrid = layout.isGrid,
			selectedFolder = layout.selectedFolder,
			filterMode = mode,
			isSearchOpen = searchOpen
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
			filterMode = ctrl.filterMode,
			isSearchOpen = ctrl.isSearchOpen,
			message = msg
		)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

	init {
		viewModelScope.launch {
			settingsRepository.settings.collect { settings ->
				sortOption.value = settings.sortOption
			}
		}

		viewModelScope.launch {
			settingsRepository.settings
				.map { it.showNomedia to it.showHiddenFiles }
				.drop(1)
				.distinctUntilChanged()
				.collect {
					if (hasAutoScanned.value) {
						refresh()
					}
				}
		}

		val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		application.contentResolver.registerContentObserver(
			videoCollection,
			true,
			mediaStoreObserver
		)

		viewModelScope.launch {
			val dbCount = withContext(Dispatchers.IO) { videoRepository.observeVideoCount().first() }
			if (dbCount > 0) {
				hasAutoScanned.value = true
			} else {
				refresh()
			}
		}
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
		query.value = ""
	}

	fun setFilterMode(mode: LibraryFilterMode) {
		filterMode.value = mode
		if (mode != LibraryFilterMode.ALL_VIDEOS) {
			selectedFolder.value = null
		}
		query.value = ""
	}

	fun toggleSearch() {
		isSearchOpen.update { !it }
		if (!isSearchOpen.value) query.value = ""
	}

	fun refresh() {
		if (isScanning.value) return
		viewModelScope.launch {
			isScanning.value = true
			scanProgress.value = 0f
			runCatching {
				scanVideos { indexed, total ->
					scanProgress.value = when {
						total <= 0 -> 0f
						else -> (indexed.toFloat() / total).coerceIn(0f, 1f)
					}
				}
			}
				.onSuccess { count -> message.value = "Indexados $count videos" }
				.onFailure { message.value = it.message ?: "Error al escanear" }
			hasAutoScanned.value = true
			isScanning.value = false
			scanProgress.value = 1f
		}
	}

	private fun checkForNewVideos() {
		viewModelScope.launch {
			val mediaStoreCount = withContext(Dispatchers.IO) {
				val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
				var count = 0
				application.contentResolver.query(
					collection,
					arrayOf(MediaStore.Video.Media._ID),
					null,
					null,
					null
				)?.use { cursor ->
					count = cursor.count
				}
				count
			}
			val dbCount = withContext(Dispatchers.IO) { videoRepository.observeVideoCount().first() }
			if (mediaStoreCount > dbCount) {
				refresh()
			}
		}
	}

	fun clearMessage() {
		message.value = null
	}

	fun onFavorite(videoId: Long) {
		viewModelScope.launch { toggleFavorite(videoId) }
	}

	fun markVideoSeen(videoId: Long) {
		viewModelScope.launch { videoRepository.markVideoSeen(videoId) }
	}

	fun createPlaylist(name: String, videoId: Long? = null) {
		viewModelScope.launch {
			val id = playlistRepository.createPlaylist(name)
			if (videoId != null) playlistRepository.addVideoToPlaylist(id, videoId)
			message.value = "Lista creada"
		}
	}

	fun deleteVideo(videoId: Long) {
		viewModelScope.launch {
			videoRepository.deleteVideo(videoId)
			message.value = "Video eliminado"
		}
	}

	fun renameVideo(videoId: Long, newName: String) {
		viewModelScope.launch {
			videoRepository.renameVideo(videoId, newName)
			message.value = "Renombrado"
		}
	}

	fun moveToPrivate(videoId: Long) {
		viewModelScope.launch {
			val settings = settingsRepository.settings.first()
			val current = settings.privateVideoIds
			if (videoId !in current) {
				settingsRepository.setPrivateVideoIds(current + videoId)
				message.value = "Movido a carpeta privada"
			}
		}
	}

	fun addToPlaylist(playlistId: Long, videoId: Long) {
		viewModelScope.launch {
			playlistRepository.addVideoToPlaylist(playlistId, videoId)
			message.value = "Agregado a la lista"
		}
	}

	val playlists = playlistRepository.observePlaylists()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	suspend fun getVideoById(videoId: Long): Video? = withContext(Dispatchers.IO) {
		videoRepository.getVideoById(videoId)
	}

	suspend fun preparePlayback(videoId: Long) {
		playbackQueue.beginLazy(
			context = PlaybackQueueContext(
				query = query.value,
				sortOption = sortOption.value,
				folder = if (filterMode.value == LibraryFilterMode.ALL_VIDEOS) null else selectedFolder.value
			),
			startId = videoId
		)
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
		val selectedFolder: String?,
		val filterMode: LibraryFilterMode,
		val isSearchOpen: Boolean
	)

	suspend fun autoPlayNext(): Boolean = withContext(Dispatchers.IO) {
		settingsRepository.settings.first().autoPlayNext
	}

	suspend fun getAllVideoIds(): List<Long> = withContext(Dispatchers.IO) {
		videoRepository.getVideoIds(
			query = query.value,
			sortOption = sortOption.value,
			folder = if (filterMode.value == LibraryFilterMode.ALL_VIDEOS) null else selectedFolder.value
		)
	}

	suspend fun getVideosByIds(ids: List<Long>): List<Video> = withContext(Dispatchers.IO) {
		videoRepository.getVideosByIds(ids)
	}

	private companion object {
		const val SEARCH_DEBOUNCE_MS = 300L
	}

	override fun onCleared() {
		super.onCleared()
		application.contentResolver.unregisterContentObserver(mediaStoreObserver)
	}
}
