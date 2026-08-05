package com.puma.videomax.presentation.music

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
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.Song
import com.puma.videomax.domain.repository.SongRepository
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.usecase.GetAlbumsUseCase
import com.puma.videomax.domain.usecase.GetArtistsUseCase
import com.puma.videomax.domain.usecase.GetSongByIdUseCase
import com.puma.videomax.domain.usecase.ObserveMusicFoldersUseCase
import com.puma.videomax.domain.usecase.ObserveSongCountUseCase
import com.puma.videomax.domain.usecase.PagingSongsUseCase
import com.puma.videomax.domain.usecase.ScanMusicUseCase
import com.puma.videomax.domain.usecase.ToggleSongFavoriteUseCase
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MusicTab {
	SONGS,
	ALBUMS,
	ARTISTS,
	FOLDERS
}

data class MusicUiState(
	val songCount: Int = 0,
	val query: String = "",
	val sortOption: MusicSortOption = MusicSortOption.DATE_DESC,
	val isScanning: Boolean = false,
	val scanProgress: Float = 0f,
	val selectedTab: MusicTab = MusicTab.SONGS,
	val selectedFolder: String? = null,
	val isSearchOpen: Boolean = false,
	val albums: List<Album> = emptyList(),
	val artists: List<Artist> = emptyList(),
	val folders: List<String> = emptyList(),
	val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
	private val application: Application,
	private val pagingSongs: PagingSongsUseCase,
	private val scanMusic: ScanMusicUseCase,
	private val toggleSongFavorite: ToggleSongFavoriteUseCase,
	private val getSongById: GetSongByIdUseCase,
	private val getAlbums: GetAlbumsUseCase,
	private val getArtists: GetArtistsUseCase,
	private val songRepository: SongRepository,
	private val settingsRepository: SettingsRepository,
	private val observeSongCount: ObserveSongCountUseCase,
	private val observeFolders: ObserveMusicFoldersUseCase
) : ViewModel() {

	private val query = MutableStateFlow("")
	private val sortOption = MutableStateFlow(MusicSortOption.DATE_DESC)
	private val isScanning = MutableStateFlow(false)
	private val scanProgress = MutableStateFlow(0f)
	private val selectedTab = MutableStateFlow(MusicTab.SONGS)
	private val selectedFolder = MutableStateFlow<String?>(null)
	private val isSearchOpen = MutableStateFlow(false)
	private val albums = MutableStateFlow<List<Album>>(emptyList())
	private val artists = MutableStateFlow<List<Artist>>(emptyList())
	private val message = MutableStateFlow<String?>(null)
	private val hasAutoScanned = MutableStateFlow(false)

	private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
		override fun onChange(selfChange: Boolean, uri: Uri?) {
			if (!isScanning.value && hasAutoScanned.value) {
				checkForNewMusic()
			}
		}
	}

	@OptIn(kotlinx.coroutines.FlowPreview::class)
	private val debouncedQuery = query
		.debounce(SEARCH_DEBOUNCE_MS)
		.distinctUntilChanged()

	val songs: Flow<PagingData<Song>> = combine(debouncedQuery, sortOption, selectedFolder) { q, sort, folder ->
		Triple(q, sort, folder)
	}.flatMapLatest { (q, sort, folder) ->
		pagingSongs(q, sort, folder)
	}.cachedIn(viewModelScope)

	private val scanState = combine(isScanning, scanProgress) { scanning, progress ->
		scanning to progress
	}

	private val uiControls = combine(query, sortOption, selectedTab, selectedFolder, isSearchOpen) { q, sort, tab, folder, searchOpen ->
		UiControls(q, sort, tab, folder, searchOpen)
	}

	val uiState: StateFlow<MusicUiState> = combine(
		observeSongCount(),
		observeFolders(),
		scanState,
		uiControls,
		albums
	) { count, folderList, scan, ctrl, albumList ->
		MusicUiState(
			songCount = count,
			query = ctrl.query,
			sortOption = ctrl.sortOption,
			isScanning = scan.first,
			scanProgress = scan.second,
			selectedTab = ctrl.tab,
			selectedFolder = ctrl.folder,
			isSearchOpen = ctrl.searchOpen,
			albums = albumList,
			artists = artists.value,
			folders = folderList,
			message = message.value
		)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MusicUiState())

	init {
		val resolver = application.contentResolver
		resolver.registerContentObserver(
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
			true,
			mediaStoreObserver
		)

		viewModelScope.launch {
			settingsRepository.settings.first().let { settings ->
				sortOption.value = settings.musicSortOption
			}
			autoScanIfNeeded()
		}
	}

	private suspend fun autoScanIfNeeded() {
		withContext(Dispatchers.IO) {
			val count = songRepository.observeSongCount().stateIn(viewModelScope).value
			if (count == 0) {
				scanLibrary()
			} else {
				hasAutoScanned.value = true
				refreshAlbumsAndArtists()
			}
		}
	}

	private fun checkForNewMusic() {
		viewModelScope.launch {
			scanLibrary()
		}
	}

	private suspend fun refreshAlbumsAndArtists() {
		withContext(Dispatchers.IO) {
			albums.value = getAlbums()
			artists.value = getArtists()
		}
	}

	fun scanLibrary() {
		if (isScanning.value) return
		viewModelScope.launch {
			isScanning.value = true
			scanProgress.value = 0f
			withContext(Dispatchers.IO) {
				scanMusic { indexed, total ->
					withContext(Dispatchers.Main) {
						scanProgress.value = if (total > 0) indexed.toFloat() / total else 0f
					}
				}
			}
			isScanning.value = false
			scanProgress.value = 1f
			hasAutoScanned.value = true
			refreshAlbumsAndArtists()
		}
	}

	fun setQuery(value: String) { query.value = value }
	fun setSortOption(value: MusicSortOption) {
		sortOption.value = value
		viewModelScope.launch {
			settingsRepository.setMusicSortOption(value)
		}
	}
	fun setSelectedTab(tab: MusicTab) { selectedTab.value = tab }
	fun setSelectedFolder(folder: String?) { selectedFolder.value = folder }

	fun selectFolderAndShowSongs(folder: String?) {
		selectedFolder.value = folder
		selectedTab.value = MusicTab.SONGS
	}

	fun clearFolderFilter() {
		selectedFolder.value = null
	}
	fun setSearchOpen(open: Boolean) { isSearchOpen.value = open }
	fun clearMessage() { message.value = null }

	fun toggleFavorite(song: Song) {
		viewModelScope.launch {
			toggleSongFavorite(song.id, !song.isFavorite)
			withContext(Dispatchers.IO) {
				refreshAlbumsAndArtists()
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		application.contentResolver.unregisterContentObserver(mediaStoreObserver)
	}

	private companion object {
		const val SEARCH_DEBOUNCE_MS = 300L
	}
}

private data class UiControls(
	val query: String,
	val sortOption: MusicSortOption,
	val tab: MusicTab,
	val folder: String?,
	val searchOpen: Boolean
)
