package com.example.videomax.domain.repository

import com.example.videomax.domain.model.PlaybackHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
	fun observeHistory(): Flow<List<PlaybackHistory>>
	suspend fun upsertHistory(
		videoId: Long,
		videoUri: String,
		displayName: String,
		positionMs: Long,
		durationMs: Long
	)
	suspend fun clearHistory()
	suspend fun removeHistory(videoId: Long)
}
