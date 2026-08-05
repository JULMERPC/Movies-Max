package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
	tableName = "music_queue_tracks",
	primaryKeys = ["queueId", "songId"],
	foreignKeys = [
		ForeignKey(
			entity = MusicQueueEntity::class,
			parentColumns = ["id"],
			childColumns = ["queueId"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = SongEntity::class,
			parentColumns = ["id"],
			childColumns = ["songId"],
			onDelete = ForeignKey.CASCADE
		)
	],
	indices = [Index("queueId"), Index("songId")]
)
data class MusicQueueTrackEntity(
	val queueId: Long,
	val songId: Long,
	val position: Int,
	val addedAt: Long = System.currentTimeMillis()
)
