package com.example.videomax.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Transient set of MediaStore video IDs seen during the current library scan.
 * Cleared at the start of each scan; used to delete Room rows that no longer exist
 * without loading every video ID into JVM memory.
 */
@Entity(tableName = "scan_keep_ids")
data class ScanKeepIdEntity(
	@PrimaryKey val id: Long
)
