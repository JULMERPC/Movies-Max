package com.example.videomax.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.videomax.data.local.db.dao.HistoryDao
import com.example.videomax.data.local.db.dao.PlaylistDao
import com.example.videomax.data.local.db.dao.VideoDao
import com.example.videomax.data.local.db.entity.HistoryEntity
import com.example.videomax.data.local.db.entity.PlaylistEntity
import com.example.videomax.data.local.db.entity.PlaylistVideoCrossRef
import com.example.videomax.data.local.db.entity.ScanKeepIdEntity
import com.example.videomax.data.local.db.entity.VideoEntity
import com.example.videomax.data.local.db.entity.VideoFtsEntity

@Database(
	entities = [
		VideoEntity::class,
		VideoFtsEntity::class,
		PlaylistEntity::class,
		PlaylistVideoCrossRef::class,
		HistoryEntity::class,
		ScanKeepIdEntity::class
	],
	version = 5,
	exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
	abstract fun videoDao(): VideoDao
	abstract fun playlistDao(): PlaylistDao
	abstract fun historyDao(): HistoryDao
}
