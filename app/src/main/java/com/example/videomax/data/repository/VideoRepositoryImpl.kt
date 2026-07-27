package com.example.videomax.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.videomax.data.local.db.QueueWindowSql
import com.example.videomax.data.local.db.dao.VideoDao
import com.example.videomax.data.local.db.entity.ScanKeepIdEntity
import com.example.videomax.data.local.db.entity.VideoEntity
import com.example.videomax.data.local.mediastore.MediaStoreVideoScanner
import com.example.videomax.data.mapper.toDomain
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.QueueWindow
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import com.example.videomax.data.local.db.FtsQuery
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
	@param:ApplicationContext private val context: Context,
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
			val fts = FtsQuery.fromUserInput(query)
			if (fts == null) {
				when (sortOption) {
					SortOption.DATE_DESC -> videoDao.pagingVideosDateDesc()
					SortOption.DATE_ASC -> videoDao.pagingVideosDateAsc()
					SortOption.NAME_ASC -> videoDao.pagingVideosNameAsc()
					SortOption.NAME_DESC -> videoDao.pagingVideosNameDesc()
					SortOption.DURATION_DESC -> videoDao.pagingVideosDurationDesc()
					SortOption.DURATION_ASC -> videoDao.pagingVideosDurationAsc()
					SortOption.SIZE_DESC -> videoDao.pagingVideosSizeDesc()
					SortOption.SIZE_ASC -> videoDao.pagingVideosSizeAsc()
				}
			} else {
				when (sortOption) {
					SortOption.DATE_DESC -> videoDao.pagingSearchDateDesc(fts)
					SortOption.DATE_ASC -> videoDao.pagingSearchDateAsc(fts)
					SortOption.NAME_ASC -> videoDao.pagingSearchNameAsc(fts)
					SortOption.NAME_DESC -> videoDao.pagingSearchNameDesc(fts)
					SortOption.DURATION_DESC -> videoDao.pagingSearchDurationDesc(fts)
					SortOption.DURATION_ASC -> videoDao.pagingSearchDurationAsc(fts)
					SortOption.SIZE_DESC -> videoDao.pagingSearchSizeDesc(fts)
					SortOption.SIZE_ASC -> videoDao.pagingSearchSizeAsc(fts)
				}
			}
		}.flow
			.map { paging -> paging.map { it.toDomain() } }
			.flowOn(Dispatchers.IO)

	override fun pagingVideosByFolder(folder: String, sortOption: SortOption): Flow<PagingData<Video>> =
		Pager(pagingConfig) {
			when (sortOption) {
				SortOption.DATE_DESC -> videoDao.pagingByFolderDateDesc(folder)
				SortOption.DATE_ASC -> videoDao.pagingByFolderDateAsc(folder)
				SortOption.NAME_ASC -> videoDao.pagingByFolderNameAsc(folder)
				SortOption.NAME_DESC -> videoDao.pagingByFolderNameDesc(folder)
				SortOption.DURATION_DESC -> videoDao.pagingByFolderDurationDesc(folder)
				SortOption.DURATION_ASC -> videoDao.pagingByFolderDurationAsc(folder)
				SortOption.SIZE_DESC -> videoDao.pagingByFolderSizeDesc(folder)
				SortOption.SIZE_ASC -> videoDao.pagingByFolderSizeAsc(folder)
			}
		}.flow
			.map { paging -> paging.map { it.toDomain() } }
			.flowOn(Dispatchers.IO)

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
		if (folder != null) {
			when (sortOption) {
				SortOption.DATE_DESC -> videoDao.getVideoIdsByFolderDateDesc(folder)
				SortOption.DATE_ASC -> videoDao.getVideoIdsByFolderDateAsc(folder)
				SortOption.NAME_ASC -> videoDao.getVideoIdsByFolderNameAsc(folder)
				SortOption.NAME_DESC -> videoDao.getVideoIdsByFolderNameDesc(folder)
				SortOption.DURATION_DESC -> videoDao.getVideoIdsByFolderDurationDesc(folder)
				SortOption.DURATION_ASC -> videoDao.getVideoIdsByFolderDurationAsc(folder)
				SortOption.SIZE_DESC -> videoDao.getVideoIdsByFolderSizeDesc(folder)
				SortOption.SIZE_ASC -> videoDao.getVideoIdsByFolderSizeAsc(folder)
			}
		} else {
			val fts = FtsQuery.fromUserInput(query)
			if (fts == null) {
				when (sortOption) {
					SortOption.DATE_DESC -> videoDao.getVideoIdsDateDesc()
					SortOption.DATE_ASC -> videoDao.getVideoIdsDateAsc()
					SortOption.NAME_ASC -> videoDao.getVideoIdsNameAsc()
					SortOption.NAME_DESC -> videoDao.getVideoIdsNameDesc()
					SortOption.DURATION_DESC -> videoDao.getVideoIdsDurationDesc()
					SortOption.DURATION_ASC -> videoDao.getVideoIdsDurationAsc()
					SortOption.SIZE_DESC -> videoDao.getVideoIdsSizeDesc()
					SortOption.SIZE_ASC -> videoDao.getVideoIdsSizeAsc()
				}
			} else {
				when (sortOption) {
					SortOption.DATE_DESC -> videoDao.getSearchIdsDateDesc(fts)
					SortOption.DATE_ASC -> videoDao.getSearchIdsDateAsc(fts)
					SortOption.NAME_ASC -> videoDao.getSearchIdsNameAsc(fts)
					SortOption.NAME_DESC -> videoDao.getSearchIdsNameDesc(fts)
					SortOption.DURATION_DESC -> videoDao.getSearchIdsDurationDesc(fts)
					SortOption.DURATION_ASC -> videoDao.getSearchIdsDurationAsc(fts)
					SortOption.SIZE_DESC -> videoDao.getSearchIdsSizeDesc(fts)
					SortOption.SIZE_ASC -> videoDao.getSearchIdsSizeAsc(fts)
				}
			}
		}
	}

	override suspend fun getVideoById(id: Long): Video? = videoDao.getById(id)?.toDomain()

	override suspend fun getVideosByIds(ids: List<Long>): List<Video> = withContext(Dispatchers.IO) {
		if (ids.isEmpty()) return@withContext emptyList()
		val entities = videoDao.getByIds(ids)
		val entityMap = entities.associateBy { it.id }
		ids.mapNotNull { entityMap[it]?.toDomain() }
	}

	override suspend fun getQueueWindow(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		before: Int,
		after: Int
	): QueueWindow = withContext(Dispatchers.IO) {
		val entity = videoDao.getById(anchorId)
			?: return@withContext QueueWindow(listOf(anchorId), hasMoreBefore = false, hasMoreAfter = false)
		val keys = QueueWindowSql.fromEntity(entity)
		val q = query.trim()

		val beforeRaw = if (before > 0) {
			videoDao.queryIds(
				QueueWindowSql.neighborsBefore(sortOption, folder, q, keys, before)
			)
		} else {
			emptyList()
		}
		// SQL returns closest-first in reverse library order → reverse for playlist order.
		val beforeIds = beforeRaw.asReversed()

		val afterIds = if (after > 0) {
			videoDao.queryIds(
				QueueWindowSql.neighborsAfter(sortOption, folder, q, keys, after)
			)
		} else {
			emptyList()
		}

		QueueWindow(
			ids = beforeIds + anchorId + afterIds,
			hasMoreBefore = beforeRaw.size >= before,
			hasMoreAfter = afterIds.size >= after
		)
	}

	override suspend fun getQueueNeighborsBefore(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		val entity = videoDao.getById(anchorId) ?: return@withContext emptyList()
		val keys = QueueWindowSql.fromEntity(entity)
		videoDao.queryIds(
			QueueWindowSql.neighborsBefore(sortOption, folder, query.trim(), keys, limit)
		).asReversed()
	}

	override suspend fun getQueueNeighborsAfter(
		query: String,
		sortOption: SortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		val entity = videoDao.getById(anchorId) ?: return@withContext emptyList()
		val keys = QueueWindowSql.fromEntity(entity)
		videoDao.queryIds(
			QueueWindowSql.neighborsAfter(sortOption, folder, query.trim(), keys, limit)
		)
	}

	override suspend fun getQueueFirstIds(
		query: String,
		sortOption: SortOption,
		folder: String?,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		videoDao.queryIds(
			QueueWindowSql.firstPage(sortOption, folder, query.trim(), limit)
		)
	}

	override suspend fun getQueueLastIds(
		query: String,
		sortOption: SortOption,
		folder: String?,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		videoDao.queryIds(
			QueueWindowSql.lastPage(sortOption, folder, query.trim(), limit)
		).asReversed()
	}

	/**
	 * Progressive MediaStore → Room sync:
	 * each scanner batch is filtered/merged and upserted immediately so Paging
	 * surfaces videos while the rest of the device is still being scanned.
	 *
	 * Stale cleanup uses a SQLite keep-set table + set-difference DELETE so we
	 * never load every Room / MediaStore ID into JVM memory at once.
	 */
	override suspend fun scanDeviceVideos(
		onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)?
	): Int = withContext(Dispatchers.IO) {
		val settings = settingsRepository.settings.first()
		val isFirstScan = settings.lastScanTimestamp == 0L
		var indexed = 0
		var totalHint = 0
		var receivedAny = false
		var keptAny = false

		val nomediaDirs = if (!settings.showNomedia) collectNomediaDirs() else emptySet()
		val privateIds = settings.privateVideoIds.toSet()

		videoDao.clearScanKeepIds()

		scanner.scanBatches(BATCH_SIZE).collect { batch ->
			receivedAny = true
			totalHint = batch.totalHint

			val ids = batch.videos.map { it.id }
			val userState = if (ids.isEmpty()) {
				emptyMap()
			} else {
				videoDao.getUserStatesForIds(ids).associateBy { it.id }
			}

			val merged = batch.videos.mapNotNull { fresh ->
				if (!settings.showHiddenFiles && isHidden(fresh)) return@mapNotNull null
				if (fresh.id in privateIds) return@mapNotNull null
				if (nomediaDirs.isNotEmpty() && isInNomediaDir(fresh, nomediaDirs)) return@mapNotNull null
				val state = userState[fresh.id]
				if (state != null) {
					fresh.copy(
						isFavorite = state.isFavorite,
						lastPositionMs = state.lastPositionMs,
						playCount = state.playCount,
						isNew = state.isNew
					)
				} else {
					fresh.copy(isNew = !isFirstScan)
				}
			}

			if (merged.isNotEmpty()) {
				videoDao.upsertAll(merged)
				videoDao.insertScanKeepIds(merged.map { ScanKeepIdEntity(it.id) })
				keptAny = true
				indexed += merged.size
				onProgress?.invoke(indexed, totalHint.coerceAtLeast(indexed))
			} else {
				onProgress?.invoke(indexed, totalHint.coerceAtLeast(indexed))
			}
			yield()
		}

		if (!receivedAny || !keptAny) {
			videoDao.clearAll()
			videoDao.clearScanKeepIds()
			settingsRepository.setLastScanTimestamp(System.currentTimeMillis())
			onProgress?.invoke(0, 0)
			return@withContext 0
		}

		// Single SQL set-difference: drop Room rows absent from this scan's keep-set.
		videoDao.deleteVideosNotInScanKeepSet()
		videoDao.clearScanKeepIds()
		settingsRepository.setLastScanTimestamp(System.currentTimeMillis())
		yield()

		onProgress?.invoke(indexed, indexed)
		indexed
	}

	/**
	 * Collects all directories that contain a .nomedia file.
	 * Walks the external storage tree and checks every directory,
	 * not just those starting with ".".
	 */
	private fun collectNomediaDirs(): Set<String> {
		val dirs = mutableSetOf<String>()
		val roots = listOf(
			android.os.Environment.getExternalStoragePublicDirectory(null),
			android.os.Environment.getExternalStorageDirectory()
		)
		for (root in roots) {
			if (!root.isDirectory) continue
			root.walkTopDown()
				.maxDepth(6)
				.filter { it.isDirectory }
				.forEach { dir ->
					if (dir.listFiles()?.any { it.name.equals(".nomedia", ignoreCase = true) } == true) {
						dirs.add(dir.absolutePath.lowercase())
					}
				}
		}
		return dirs
	}

	private fun isInNomediaDir(video: VideoEntity, nomediaDirs: Set<String>): Boolean {
		val path = video.path?.lowercase() ?: return false
		return nomediaDirs.any { nomediaDir ->
			path.startsWith(nomediaDir)
		}
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

	override suspend fun markVideoSeen(videoId: Long) {
		videoDao.updateIsNew(videoId, isNew = false)
	}

	override suspend fun deleteVideo(videoId: Long) = withContext(Dispatchers.IO) {
		val video = videoDao.getById(videoId) ?: return@withContext
		val uri = Uri.parse(video.uri)
		try {
			context.contentResolver.delete(uri, null, null)
		} catch (_: Exception) { }
		videoDao.deleteByIds(listOf(videoId))
	}

	override suspend fun renameVideo(videoId: Long, newName: String) = withContext(Dispatchers.IO) {
		val video = videoDao.getById(videoId) ?: return@withContext
		val uri = Uri.parse(video.uri)
		val values = ContentValues().apply {
			put(MediaStore.Video.Media.DISPLAY_NAME, newName)
		}
		try {
			context.contentResolver.update(uri, values, null, null)
		} catch (_: Exception) { }
		videoDao.updateDisplayName(videoId, newName)
	}

	/** Path-based only — avoids per-file filesystem I/O during scan. */
	private fun isHidden(video: VideoEntity): Boolean {
		if (video.displayName.startsWith(".")) return true
		val path = video.path ?: return false
		return path.split('/', '\\').any { segment ->
			segment.isNotEmpty() && segment.startsWith(".")
		}
	}

	private companion object {
		const val BATCH_SIZE = 64
	}
}
