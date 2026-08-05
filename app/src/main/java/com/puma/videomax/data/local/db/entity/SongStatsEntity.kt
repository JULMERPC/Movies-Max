package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "song_stats",
	foreignKeys = [
		ForeignKey(
			entity = SongEntity::class,
			parentColumns = ["id"],
			childColumns = ["songId"],
			onDelete = ForeignKey.CASCADE
		)
	],
	indices = [Index("songId")]
)
data class SongStatsEntity(
	@PrimaryKey val songId: Long,
	val rating: Int = 0,
	val lastPositionMs: Long = 0L,
	val audioTrackId: String? = null
)
