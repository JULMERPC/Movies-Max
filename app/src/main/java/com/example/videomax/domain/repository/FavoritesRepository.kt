package com.example.videomax.domain.repository

import com.example.videomax.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
	fun observeFavorites(): Flow<List<Video>>
	suspend fun toggleFavorite(videoId: Long)
	suspend fun isFavorite(videoId: Long): Boolean
}
