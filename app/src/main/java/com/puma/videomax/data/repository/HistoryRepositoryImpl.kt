package com.puma.videomax.data.repository

import com.puma.videomax.data.local.db.dao.HistoryDao
import com.puma.videomax.data.local.db.entity.HistoryEntity
import com.puma.videomax.data.mapper.toDomain
import com.puma.videomax.domain.model.PlaybackHistory
import com.puma.videomax.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
	private val historyDao: HistoryDao
) : HistoryRepository {

	override fun observeHistory(): Flow<List<PlaybackHistory>> =
		historyDao.observeAll().map { list -> list.map { it.toDomain() } }

	override suspend fun upsertHistory(
		videoId: Long,
		videoUri: String,
		displayName: String,
		positionMs: Long,
		durationMs: Long
	) {
		historyDao.upsert(
			HistoryEntity(
				videoId = videoId,
				videoUri = videoUri,
				displayName = displayName,
				positionMs = positionMs,
				durationMs = durationMs,
				watchedAt = System.currentTimeMillis()
			)
		)
	}

	override suspend fun clearHistory() {
		historyDao.clearAll()
	}

	override suspend fun removeHistory(videoId: Long) {
		historyDao.delete(videoId)
	}
}
