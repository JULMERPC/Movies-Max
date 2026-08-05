package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "songs",
	indices = [
		Index(value = ["title"]),
		Index(value = ["artist"]),
		Index(value = ["album"]),
		Index(value = ["albumId"]),
		Index(value = ["folderName"]),
		Index(value = ["dateAdded"]),
		Index(value = ["isFavorite"]),
		Index(value = ["playCount"]),
		Index(value = ["durationMs"]),
		Index(value = ["sizeBytes"]),
		Index(value = ["year"]),
		Index(value = ["trackNumber"]),
		Index(value = ["discNumber"])
	]
)
data class SongEntity(
	@PrimaryKey val id: Long,
	val uri: String,
	val title: String,
	val artist: String,
	val album: String,
	val albumId: Long,
	val durationMs: Long,
	val sizeBytes: Long,
	val mimeType: String,
	val dateAdded: Long,
	val dateModified: Long,
	val path: String?,
	val folderName: String,
	val trackNumber: Int,
	val discNumber: Int,
	val year: Int,
	val bitrate: Int,
	val sampleRate: Int,
	val isFavorite: Boolean = false,
	val lastPositionMs: Long = 0L,
	val playCount: Int = 0,
	val trackGain: Float? = null,
	val albumGain: Float? = null,
	val trackPeak: Float? = null,
	val albumPeak: Float? = null
)
