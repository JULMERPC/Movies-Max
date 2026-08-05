package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class HistoryEntity(
	@PrimaryKey val videoId: Long,
	val videoUri: String,
	val displayName: String,
	val positionMs: Long,
	val durationMs: Long,
	val watchedAt: Long
)
