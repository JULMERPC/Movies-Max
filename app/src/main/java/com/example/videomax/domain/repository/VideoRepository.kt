package com.example.videomax.domain.repository

import androidx.paging.PagingData
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import kotlinx.coroutines.flow.Flow

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
	suspend fun scanDeviceVideos(onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)? = null): Int
	suspend fun updateFavorite(videoId: Long, isFavorite: Boolean)
	suspend fun updateLastPosition(videoId: Long, positionMs: Long)
	suspend fun incrementPlayCount(videoId: Long)
}
