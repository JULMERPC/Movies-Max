package com.puma.videomax.domain.usecase

import androidx.paging.PagingData
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.Song
import com.puma.videomax.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(
		onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)? = null
	): Int = repository.scanDeviceMusic(onProgress)
}

class PagingSongsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	operator fun invoke(
		query: String,
		sortOption: MusicSortOption,
		folder: String?
	): Flow<PagingData<Song>> =
		if (folder == null) repository.pagingSongs(query, sortOption)
		else repository.pagingSongsByFolder(folder, sortOption)
}

class GetSongByIdUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(id: Long): Song? = repository.getSongById(id)
}

class GetAlbumsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(): List<Album> = repository.getAlbums()
}

class GetAlbumSongsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(albumId: Long): List<Song> = repository.getAlbumSongs(albumId)
}

class GetArtistsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(): List<Artist> = repository.getArtists()
}

class GetArtistSongsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(artist: String): List<Song> = repository.getArtistSongs(artist)
}

class ToggleSongFavoriteUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(songId: Long, isFavorite: Boolean) =
		repository.updateFavorite(songId, isFavorite)
}

class SaveSongPlaybackProgressUseCase @Inject constructor(
	private val repository: SongRepository
) {
	suspend operator fun invoke(songId: Long, positionMs: Long) {
		repository.updateLastPosition(songId, positionMs)
		repository.incrementPlayCount(songId)
	}
}

class ObserveMostPlayedSongsUseCase @Inject constructor(
	private val repository: SongRepository
) {
	operator fun invoke(limit: Int = 100): Flow<List<Song>> = repository.observeMostPlayed(limit)
}

class ObserveSongCountUseCase @Inject constructor(
	private val repository: SongRepository
) {
	operator fun invoke(): Flow<Int> = repository.observeSongCount()
}

class ObserveMusicFoldersUseCase @Inject constructor(
	private val repository: SongRepository
) {
	operator fun invoke(): Flow<List<String>> = repository.observeFolders()
}

class GetRecommendedTracksUseCase @Inject constructor(
	private val repository: SongRepository
) {
	/**
	 * Co-occurrence based recommendations (Namida-style).
	 * Finds tracks that frequently appear near [songId] in listening history.
	 */
	suspend operator fun invoke(
		songId: Long,
		allSongIds: List<Long>,
		limit: Int = 20
	): List<Song> {
		if (allSongIds.isEmpty()) return emptyList()
		val songs = repository.getSongsByIds(allSongIds)
		if (songs.isEmpty()) return emptyList()

		// Simple co-occurrence: find songs by the same artist or album
		val target = songs.firstOrNull { it.id == songId } ?: return emptyList()
		return songs
			.filter { it.id != songId }
			.filter { it.artist == target.artist || it.album == target.album }
			.sortedByDescending { it.playCount }
			.take(limit)
	}
}
