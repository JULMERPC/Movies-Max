package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "music_queues",
	indices = [Index("createdAt")]
)
data class MusicQueueEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val name: String,
	val createdAt: Long = System.currentTimeMillis(),
	val isFavorite: Boolean = false,
	val sourceType: String = "manual",
	val sourceId: String? = null
)
