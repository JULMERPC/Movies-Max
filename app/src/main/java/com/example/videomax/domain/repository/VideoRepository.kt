package com.example.videomax.domain.repository

import androidx.paging.PagingData
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import kotlinx.coroutines.flow.Flow

data class QueueWindow(
	val ids: List<Long>,
	val hasMoreBefore: Boolean,
	val hasMoreAfter: Boolean
)

interface VideoRepository {
	fun pagingVideos(query: String, sortOption: SortOption): Flow<PagingData<Video>>
	fun pagingVideosByFolder(folder: String, sortOption: SortOption): Flow<PagingData<Video>>
	fun observeFolders(): Flow<List<String>>
	fun observeVideoCount(): Flow<Int>
	fun observeMostPlayed(limit: Int = 100): Flow<List<Video>>
	fun pagingMostPlayed(): Flow<PagingData<Video>>
	fun pagingFavorites(): Flow<PagingData<Video>>
	suspend fun getVideoIds(query: String, sortOption: SortOption, folder: String?): List<Long>
	suspend fun getVideoById(id: Long): Video?
	suspend fun getVideosByIds(ids: List<Long>): List<Video>

	/**
	 * Lazy queue window around [anchorId] in Library order.
	 * Returns at most [before] + 1 + [after] IDs without loading the full library.
	 */
	suspend fun getQueueWindow(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		before: Int,
		after: Int
	): QueueWindow

	/** IDs immediately before [anchorId] in Library order (closest-first; reverse for playlist). */
	suspend fun getQueueNeighborsBefore(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long>

	/** IDs immediately after [anchorId] in Library order. */
	suspend fun getQueueNeighborsAfter(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long>

	/** First [limit] IDs of the filtered/sorted library (for Repeat All wrap). */
	suspend fun getQueueFirstIds(
		query: String,
		sortOption: SortOption,
		folder: String?,
		limit: Int
	): List<Long>

	/** Last [limit] IDs of the filtered/sorted library (for Repeat All previous wrap). */
	suspend fun getQueueLastIds(
		query: String,
		sortOption: SortOption,
		folder: String?,
		limit: Int
	): List<Long>

	suspend fun scanDeviceVideos(onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)? = null): Int
	suspend fun updateFavorite(videoId: Long, isFavorite: Boolean)
	suspend fun updateLastPosition(videoId: Long, positionMs: Long)
	suspend fun incrementPlayCount(videoId: Long)
}
