package com.example.videomax.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
	version = 6,
	exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
	abstract fun videoDao(): VideoDao
	abstract fun playlistDao(): PlaylistDao
	abstract fun historyDao(): HistoryDao

	companion object {
		/**
		 * Migration from version 5 to 6.
		 * Empty — establishes the migration pattern for future schema changes.
		 * Always add a new Migration() here instead of using destructive fallback.
		 */
		val MIGRATION_5_6 = object : Migration(5, 6) {
			override fun migrate(db: SupportSQLiteDatabase) {
				// No schema changes — placeholder for future upgrades.
			}
		}
	}
}
