package com.example.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val name: String,
	val createdAt: Long = System.currentTimeMillis()
)

@Entity(
	tableName = "playlist_videos",
	primaryKeys = ["playlistId", "videoId"],
	foreignKeys = [
		ForeignKey(
			entity = PlaylistEntity::class,
			parentColumns = ["id"],
			childColumns = ["playlistId"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = VideoEntity::class,
			parentColumns = ["id"],
			childColumns = ["videoId"],
			onDelete = ForeignKey.CASCADE
		)
	],
	indices = [Index("playlistId"), Index("videoId")]
)
data class PlaylistVideoCrossRef(
	val playlistId: Long,
	val videoId: Long,
	val addedAt: Long = System.currentTimeMillis()
)
