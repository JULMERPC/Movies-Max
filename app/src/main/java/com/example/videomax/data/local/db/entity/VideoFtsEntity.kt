package com.example.videomax.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text index over video names and folders.
 * Kept in sync with [VideoEntity] via Room content-entity triggers.
 */
@Fts4(contentEntity = VideoEntity::class)
@Entity(tableName = "videos_fts")
data class VideoFtsEntity(
	@ColumnInfo(name = "displayName")
	val displayName: String,
	@ColumnInfo(name = "folderName")
	val folderName: String
)
