package com.example.videomax.data.repository

import com.example.videomax.data.local.db.dao.VideoDao
import com.example.videomax.data.local.mediastore.MediaStoreVideoScanner
import com.example.videomax.data.mapper.toDomain
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
	private val videoDao: VideoDao,
	private val scanner: MediaStoreVideoScanner
) : VideoRepository {

	override fun observeVideos(query: String, sortOption: SortOption): Flow<List<Video>> =
		videoDao.observeVideos(query.trim(), sortOption.name)
			.map { list -> list.map { it.toDomain() } }

	override fun observeFolders(): Flow<List<String>> = videoDao.observeFolders()

	override fun observeVideosByFolder(folder: String, sortOption: SortOption): Flow<List<Video>> =
		videoDao.observeByFolder(folder, sortOption.name)
			.map { list -> list.map { it.toDomain() } }

	override suspend fun getVideoById(id: Long): Video? = videoDao.getById(id)?.toDomain()

	override suspend fun scanDeviceVideos(): Int {
		val scanned = scanner.scan()
		if (scanned.isEmpty()) {
			videoDao.clearAll()
			return 0
		}

		val userState = videoDao.getUserStates().associateBy { it.id }
		val merged = scanned.map { fresh ->
			val state = userState[fresh.id]
			if (state != null) {
				fresh.copy(
					isFavorite = state.isFavorite,
					lastPositionMs = state.lastPositionMs
				)
			} else {
				fresh
			}
		}

		merged.chunked(500).forEach { chunk -> videoDao.upsertAll(chunk) }

		val keepIds = merged.map { it.id }.toHashSet()
		val staleIds = videoDao.getAllIds().filter { it !in keepIds }
		staleIds.chunked(500).forEach { chunk -> videoDao.deleteByIds(chunk) }

		return merged.size
	}

	override suspend fun updateFavorite(videoId: Long, isFavorite: Boolean) {
		videoDao.updateFavorite(videoId, isFavorite)
	}

	override suspend fun updateLastPosition(videoId: Long, positionMs: Long) {
		videoDao.updateLastPosition(videoId, positionMs)
	}
}
