package com.example.videomax.presentation.playlists

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.PlaybackHistory
import com.example.videomax.domain.model.Playlist
import com.example.videomax.domain.model.PlaylistWithVideos
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.HistoryRepository
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.domain.usecase.CreatePlaylistUseCase
import com.example.videomax.domain.usecase.GetVideoByIdUseCase
import com.example.videomax.domain.usecase.ObserveFavoritesUseCase
import com.example.videomax.domain.usecase.ObserveHistoryUseCase
import com.example.videomax.domain.usecase.ObservePlaylistsUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.presentation.navigation.Screen
import com.example.videomax.presentation.player.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartCategory(
	val type: String,
	val title: String,
	val subtitle: String,
	val icon: ImageVector
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
	observePlaylists: ObservePlaylistsUseCase,
	private val createPlaylist: CreatePlaylistUseCase,
	private val playlistRepository: PlaylistRepository,
	observeFavorites: ObserveFavoritesUseCase,
	observeHistory: ObserveHistoryUseCase,
	videoRepository: VideoRepository
) : ViewModel() {
	val playlists: StateFlow<List<Playlist>> = observePlaylists()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val favoritesCount: StateFlow<Int> = observeFavorites()
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	val historyCount: StateFlow<Int> = observeHistory()
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	val mostPlayedCount: StateFlow<Int> = videoRepository.observeMostPlayed(200)
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	fun create(name: String) {
		viewModelScope.launch { createPlaylist(name) }
	}

	fun delete(id: Long) {
		viewModelScope.launch { playlistRepository.deletePlaylist(id) }
	}
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	playlistRepository: PlaylistRepository,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

	val playlist: StateFlow<PlaylistWithVideos?> =
		playlistRepository.observePlaylistWithVideos(playlistId)
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	fun preparePlayback(videoId: Long) {
		val ids = playlist.value?.videos?.map { it.id }.orEmpty()
		playbackQueue.setQueue(ids, videoId)
	}
}

@HiltViewModel
class SmartCollectionViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	observeFavorites: ObserveFavoritesUseCase,
	observeHistory: ObserveHistoryUseCase,
	videoRepository: VideoRepository,
	private val getVideoById: GetVideoByIdUseCase,
	private val historyRepository: HistoryRepository,
	private val toggleFavorite: ToggleFavoriteUseCase,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	val type: String = checkNotNull(savedStateHandle["type"])

	val favorites: StateFlow<List<Video>> = observeFavorites()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val history: StateFlow<List<PlaybackHistory>> = observeHistory()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val mostPlayed: StateFlow<List<Video>> = videoRepository.observeMostPlayed(100)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	private val _queueVideos = MutableStateFlow<List<Video>>(emptyList())
	val queueVideos: StateFlow<List<Video>> = _queueVideos.asStateFlow()

	init {
		if (type == Screen.SmartCollection.QUEUE) {
			viewModelScope.launch { refreshQueue() }
		}
	}

	private suspend fun refreshQueue() {
		_queueVideos.value = playbackQueue.ids().mapNotNull { getVideoById(it) }
	}

	fun toggleFavorite(videoId: Long) {
		viewModelScope.launch { toggleFavorite(videoId) }
	}

	fun clearHistory() {
		viewModelScope.launch { historyRepository.clearHistory() }
	}

	fun prepareFromVideos(videos: List<Video>, videoId: Long) {
		playbackQueue.setQueue(videos.map { it.id }, videoId)
	}

	fun prepareFromHistory(items: List<PlaybackHistory>, videoId: Long) {
		playbackQueue.setQueue(items.map { it.videoId }, videoId)
	}

	fun prepareFromQueue(videoId: Long) {
		playbackQueue.ensureSingle(videoId)
	}
}
