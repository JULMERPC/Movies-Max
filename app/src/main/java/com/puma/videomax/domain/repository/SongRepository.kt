package com.puma.videomax.domain.repository

import androidx.paging.PagingData
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.Song
import kotlinx.coroutines.flow.Flow

data class MusicQueueWindow(
	val ids: List<Long>,
	val hasMoreBefore: Boolean,
	val hasMoreAfter: Boolean
)

interface SongRepository {
	fun pagingSongs(query: String, sortOption: MusicSortOption): Flow<PagingData<Song>>
	fun pagingSongsByFolder(folder: String, sortOption: MusicSortOption): Flow<PagingData<Song>>
	fun observeFolders(): Flow<List<String>>
	fun observeSongCount(): Flow<Int>
	fun observeMostPlayed(limit: Int = 100): Flow<List<Song>>
	fun pagingMostPlayed(): Flow<PagingData<Song>>
	fun pagingFavorites(): Flow<PagingData<Song>>
	suspend fun getSongIds(query: String, sortOption: MusicSortOption, folder: String?): List<Long>
	suspend fun getSongById(id: Long): Song?
	suspend fun getSongsByIds(ids: List<Long>): List<Song>
	suspend fun getAlbums(): List<Album>
	suspend fun getAlbumSongs(albumId: Long): List<Song>
	suspend fun getArtists(): List<Artist>
	suspend fun getArtistSongs(artist: String): List<Song>

	suspend fun getMusicQueueWindow(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		before: Int,
		after: Int
	): MusicQueueWindow

	suspend fun getMusicQueueNeighborsBefore(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long>

	suspend fun getMusicQueueNeighborsAfter(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long>

	suspend fun getMusicQueueFirstIds(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		limit: Int
	): List<Long>

	suspend fun getMusicQueueLastIds(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		limit: Int
	): List<Long>

	suspend fun scanDeviceMusic(onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)? = null): Int
	suspend fun updateFavorite(songId: Long, isFavorite: Boolean)
	suspend fun updateLastPosition(songId: Long, positionMs: Long)
	suspend fun incrementPlayCount(songId: Long)
	suspend fun deleteSong(songId: Long)
	suspend fun renameSong(songId: Long, newTitle: String)
}
