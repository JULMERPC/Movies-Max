package com.puma.videomax.data.repository

import com.puma.videomax.data.local.db.dao.VideoDao
import com.puma.videomax.data.mapper.toDomain
import com.puma.videomax.domain.model.Video
import com.puma.videomax.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
	private val videoDao: VideoDao
) : FavoritesRepository {

	override fun observeFavorites(): Flow<List<Video>> =
		videoDao.observeFavorites().map { list -> list.map { it.toDomain() } }

	override suspend fun toggleFavorite(videoId: Long) {
		// Guarded: a heart tap must never crash the app ("me bota de la app").
		// Single atomic statement — no read-then-write lost updates either.
		runCatching { videoDao.toggleFavorite(videoId) }
	}

	override suspend fun isFavorite(videoId: Long): Boolean =
		videoDao.isFavorite(videoId) ?: false
}
