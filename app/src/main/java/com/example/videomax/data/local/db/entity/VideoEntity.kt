package com.example.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "videos",
	indices = [
		Index(value = ["displayName"]),
		Index(value = ["folderName"]),
		Index(value = ["dateAdded"]),
		Index(value = ["isFavorite"]),
		Index(value = ["playCount"]),
		Index(value = ["durationMs"]),
		Index(value = ["sizeBytes"])
	]
)
data class VideoEntity(
	@PrimaryKey val id: Long,
	val uri: String,
	val displayName: String,
	val path: String?,
	val durationMs: Long,
	val sizeBytes: Long,
	val width: Int,
	val height: Int,
	val mimeType: String,
	val dateAdded: Long,
	val dateModified: Long,
	val folderName: String,
	val isFavorite: Boolean = false,
	val lastPositionMs: Long = 0L,
	val codec: String? = null,
	val playCount: Int = 0,
	val isNew: Boolean = false
)
