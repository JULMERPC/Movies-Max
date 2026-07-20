package com.example.videomax.domain.repository

import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
	fun observeVideos(query: String, sortOption: SortOption): Flow<List<Video>>
	fun observeFolders(): Flow<List<String>>
	fun observeVideosByFolder(folder: String, sortOption: SortOption): Flow<List<Video>>
	suspend fun getVideoById(id: Long): Video?
	suspend fun scanDeviceVideos(): Int
	suspend fun updateFavorite(videoId: Long, isFavorite: Boolean)
	suspend fun updateLastPosition(videoId: Long, positionMs: Long)
}
