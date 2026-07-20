package com.example.videomax.domain.usecase

import androidx.paging.PagingData
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanVideosUseCase @Inject constructor(
	private val repository: VideoRepository
) {
	suspend operator fun invoke(
		onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)? = null
	): Int = repository.scanDeviceVideos(onProgress)
}

class PagingVideosUseCase @Inject constructor(
	private val repository: VideoRepository
) {
	operator fun invoke(
		query: String,
		sortOption: SortOption,
		folder: String?
	): Flow<PagingData<Video>> =
		if (folder == null) repository.pagingVideos(query, sortOption)
		else repository.pagingVideosByFolder(folder, sortOption)
}

class GetVideoByIdUseCase @Inject constructor(
	private val repository: VideoRepository
) {
	suspend operator fun invoke(id: Long): Video? = repository.getVideoById(id)
}

class ToggleFavoriteUseCase @Inject constructor(
	private val favoritesRepository: com.example.videomax.domain.repository.FavoritesRepository
) {
	suspend operator fun invoke(videoId: Long) = favoritesRepository.toggleFavorite(videoId)
}

class ObserveFavoritesUseCase @Inject constructor(
	private val favoritesRepository: com.example.videomax.domain.repository.FavoritesRepository
) {
	operator fun invoke() = favoritesRepository.observeFavorites()
}

class ObserveHistoryUseCase @Inject constructor(
	private val historyRepository: com.example.videomax.domain.repository.HistoryRepository
) {
	operator fun invoke() = historyRepository.observeHistory()
}

class SavePlaybackProgressUseCase @Inject constructor(
	private val historyRepository: com.example.videomax.domain.repository.HistoryRepository,
	private val videoRepository: VideoRepository
) {
	suspend operator fun invoke(
		videoId: Long,
		videoUri: String,
		displayName: String,
		positionMs: Long,
		durationMs: Long
	) {
		historyRepository.upsertHistory(videoId, videoUri, displayName, positionMs, durationMs)
		videoRepository.updateLastPosition(videoId, positionMs)
	}
}

class ObserveSettingsUseCase @Inject constructor(
	private val settingsRepository: com.example.videomax.domain.repository.SettingsRepository
) {
	operator fun invoke() = settingsRepository.settings
}

class ObservePlaylistsUseCase @Inject constructor(
	private val playlistRepository: com.example.videomax.domain.repository.PlaylistRepository
) {
	operator fun invoke() = playlistRepository.observePlaylists()
}

class CreatePlaylistUseCase @Inject constructor(
	private val playlistRepository: com.example.videomax.domain.repository.PlaylistRepository
) {
	suspend operator fun invoke(name: String): Long = playlistRepository.createPlaylist(name)
}

class AddVideoToPlaylistUseCase @Inject constructor(
	private val playlistRepository: com.example.videomax.domain.repository.PlaylistRepository
) {
	suspend operator fun invoke(playlistId: Long, videoId: Long) =
		playlistRepository.addVideoToPlaylist(playlistId, videoId)
}
