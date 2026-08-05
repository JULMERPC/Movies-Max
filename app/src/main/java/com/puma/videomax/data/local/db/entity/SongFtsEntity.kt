package com.puma.videomax.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text index over song title, artist, album, and folder.
 * Kept in sync with [SongEntity] via Room content-entity triggers.
 */
@Fts4(contentEntity = SongEntity::class)
@Entity(tableName = "songs_fts")
data class SongFtsEntity(
	@ColumnInfo(name = "title")
	val title: String,
	@ColumnInfo(name = "artist")
	val artist: String,
	@ColumnInfo(name = "album")
	val album: String,
	@ColumnInfo(name = "folderName")
	val folderName: String
)
