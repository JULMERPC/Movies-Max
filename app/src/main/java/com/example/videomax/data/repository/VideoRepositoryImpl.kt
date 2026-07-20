package com.example.videomax.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.videomax.data.local.db.dao.VideoDao
import com.example.videomax.data.local.db.entity.VideoEntity
import com.example.videomax.data.local.mediastore.MediaStoreVideoScanner
import com.example.videomax.data.mapper.toDomain
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
	private val videoDao: VideoDao,
	private val scanner: MediaStoreVideoScanner,
	private val settingsRepository: SettingsRepository
) : VideoRepository {

	private val pagingConfig = PagingConfig(
		pageSize = 40,
		prefetchDistance = 20,
		enablePlaceholders = false,
		initialLoadSize = 60
	)

	override fun pagingVideos(query: String, sortOption: SortOption): Flow<PagingData<Video>> =
		Pager(pagingConfig) {
			videoDao.pagingVideos(query.trim(), sortOption.name)
		}.flow.map { paging -> paging.map { it.toDomain() } }

	override fun pagingVideosByFolder(folder: String, sortOption: SortOption): Flow<PagingData<Video>> =
		Pager(pagingConfig) {
			videoDao.pagingByFolder(folder, sortOption.name)
		}.flow.map { paging -> paging.map { it.toDomain() } }

	override fun observeFolders(): Flow<List<String>> = videoDao.observeFolders()

	override fun observeVideoCount(): Flow<Int> = videoDao.observeCount()

	override fun observeMostPlayed(limit: Int): Flow<List<Video>> =
		videoDao.observeMostPlayed(limit).map { list -> list.map { it.toDomain() } }

	override fun pagingMostPlayed(): Flow<PagingData<Video>> =
		Pager(pagingConfig) { videoDao.pagingMostPlayed() }
			.flow.map { paging -> paging.map { it.toDomain() } }

	override fun pagingFavorites(): Flow<PagingData<Video>> =
		Pager(pagingConfig) { videoDao.pagingFavorites() }
			.flow.map { paging -> paging.map { it.toDomain() } }

	override suspend fun getVideoIds(
		query: String,
		sortOption: SortOption,
		folder: String?
	): List<Long> = withContext(Dispatchers.IO) {
		if (folder == null) {
			videoDao.getVideoIds(query.trim(), sortOption.name)
		} else {
			videoDao.getVideoIdsByFolder(folder, sortOption.name)
		}
	}

	override suspend fun getVideoById(id: Long): Video? = videoDao.getById(id)?.toDomain()

	override suspend fun scanDeviceVideos(
		onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)?
	): Int = withContext(Dispatchers.IO) {
		val scanned = scanner.scan()
		if (scanned.isEmpty()) {
			videoDao.clearAll()
			onProgress?.invoke(0, 0)
			return@withContext 0
		}

		val userState = videoDao.getUserStates().associateBy { it.id }
		val settings = settingsRepository.settings.first()
		val merged = scanned.mapNotNull { fresh ->
			if (!settings.showHiddenFiles && isHidden(fresh)) return@mapNotNull null
			if (isBlacklisted(fresh, settings.blacklist)) return@mapNotNull null
			val state = userState[fresh.id]
			if (state != null) {
				fresh.copy(
					isFavorite = state.isFavorite,
					lastPositionMs = state.lastPositionMs,
					playCount = state.playCount
				)
			} else {
				fresh
			}
		}

		var indexed = 0
		val total = merged.size
		merged.chunked(120).forEach { chunk ->
			videoDao.upsertAll(chunk)
			indexed += chunk.size
			onProgress?.invoke(indexed, total)
			yield()
		}

		val keepIds = merged.map { it.id }.toHashSet()
		val staleIds = videoDao.getAllIds().filter { it !in keepIds }
		staleIds.chunked(500).forEach { chunk ->
			videoDao.deleteByIds(chunk)
			yield()
		}

		indexed
	}

	override suspend fun updateFavorite(videoId: Long, isFavorite: Boolean) {
		videoDao.updateFavorite(videoId, isFavorite)
	}

	override suspend fun updateLastPosition(videoId: Long, positionMs: Long) {
		videoDao.updateLastPosition(videoId, positionMs)
	}

	override suspend fun incrementPlayCount(videoId: Long) {
		videoDao.incrementPlayCount(videoId)
	}

	private fun isHidden(video: VideoEntity): Boolean {
		if (video.displayName.startsWith(".")) return true
		val path = video.path ?: return false
		return path.split('/', '\\').any { segment ->
			segment.isNotEmpty() && segment.startsWith(".")
		} || File(path).isHidden
	}

	private fun isBlacklisted(video: VideoEntity, blacklist: List<String>): Boolean {
		if (blacklist.isEmpty()) return false
		return blacklist.any { entry ->
			video.displayName.contains(entry, ignoreCase = true) ||
				video.folderName.contains(entry, ignoreCase = true) ||
				video.path?.contains(entry, ignoreCase = true) == true
		}
	}
}
