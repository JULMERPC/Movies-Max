package com.puma.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Transient set of MediaStore song IDs seen during the current library scan.
 * Cleared at the start of each scan; used to delete Room rows that no longer exist
 * without loading every song ID into JVM memory.
 */
@Entity(tableName = "scan_keep_song_ids")
data class ScanKeepSongIdEntity(
	@PrimaryKey val id: Long
)
