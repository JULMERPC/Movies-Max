package com.puma.videomax.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.puma.videomax.data.local.db.dao.HistoryDao
import com.puma.videomax.data.local.db.dao.MusicQueueDao
import com.puma.videomax.data.local.db.dao.PlaylistDao
import com.puma.videomax.data.local.db.dao.SongDao
import com.puma.videomax.data.local.db.dao.SongStatsDao
import com.puma.videomax.data.local.db.dao.VideoDao
import com.puma.videomax.data.local.db.entity.HistoryEntity
import com.puma.videomax.data.local.db.entity.MusicQueueEntity
import com.puma.videomax.data.local.db.entity.MusicQueueTrackEntity
import com.puma.videomax.data.local.db.entity.PlaylistEntity
import com.puma.videomax.data.local.db.entity.PlaylistVideoCrossRef
import com.puma.videomax.data.local.db.entity.ScanKeepIdEntity
import com.puma.videomax.data.local.db.entity.ScanKeepSongIdEntity
import com.puma.videomax.data.local.db.entity.SongEntity
import com.puma.videomax.data.local.db.entity.SongFtsEntity
import com.puma.videomax.data.local.db.entity.SongStatsEntity
import com.puma.videomax.data.local.db.entity.VideoEntity
import com.puma.videomax.data.local.db.entity.VideoFtsEntity

@Database(
	entities = [
		VideoEntity::class,
		VideoFtsEntity::class,
		PlaylistEntity::class,
		PlaylistVideoCrossRef::class,
		HistoryEntity::class,
		ScanKeepIdEntity::class,
		SongEntity::class,
		SongFtsEntity::class,
		SongStatsEntity::class,
		MusicQueueEntity::class,
		MusicQueueTrackEntity::class,
		ScanKeepSongIdEntity::class
	],
	version = 8,
	exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
	abstract fun videoDao(): VideoDao
	abstract fun playlistDao(): PlaylistDao
	abstract fun historyDao(): HistoryDao
	abstract fun songDao(): SongDao
	abstract fun songStatsDao(): SongStatsDao
	abstract fun musicQueueDao(): MusicQueueDao

	companion object {
		val MIGRATION_5_6 = object : Migration(5, 6) {
			override fun migrate(db: SupportSQLiteDatabase) {
				// No schema changes — placeholder for future upgrades.
			}
		}

		val MIGRATION_6_7 = object : Migration(6, 7) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE videos ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0")
			}
		}

		val MIGRATION_7_8 = object : Migration(7, 8) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS songs (
						id INTEGER PRIMARY KEY NOT NULL,
						uri TEXT NOT NULL,
						title TEXT NOT NULL,
						artist TEXT NOT NULL DEFAULT '',
						album TEXT NOT NULL DEFAULT '',
						albumId INTEGER NOT NULL DEFAULT -1,
						durationMs INTEGER NOT NULL DEFAULT 0,
						sizeBytes INTEGER NOT NULL DEFAULT 0,
						mimeType TEXT NOT NULL DEFAULT 'audio/mpeg',
						dateAdded INTEGER NOT NULL DEFAULT 0,
						dateModified INTEGER NOT NULL DEFAULT 0,
						path TEXT,
						folderName TEXT NOT NULL DEFAULT '',
						trackNumber INTEGER NOT NULL DEFAULT 0,
						discNumber INTEGER NOT NULL DEFAULT 0,
						year INTEGER NOT NULL DEFAULT 0,
						bitrate INTEGER NOT NULL DEFAULT 0,
						sampleRate INTEGER NOT NULL DEFAULT 0,
						isFavorite INTEGER NOT NULL DEFAULT 0,
						lastPositionMs INTEGER NOT NULL DEFAULT 0,
						playCount INTEGER NOT NULL DEFAULT 0,
						trackGain REAL DEFAULT NULL,
						albumGain REAL DEFAULT NULL,
						trackPeak REAL DEFAULT NULL,
						albumPeak REAL DEFAULT NULL
					)
					"""
				)
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_title ON songs (title)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_artist ON songs (artist)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_album ON songs (album)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_albumId ON songs (albumId)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_folderName ON songs (folderName)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_dateAdded ON songs (dateAdded)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isFavorite ON songs (isFavorite)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_playCount ON songs (playCount)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_durationMs ON songs (durationMs)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_sizeBytes ON songs (sizeBytes)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_year ON songs (year)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_trackNumber ON songs (trackNumber)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_discNumber ON songs (discNumber)")

				db.execSQL(
					"""
					CREATE VIRTUAL TABLE IF NOT EXISTS songs_fts USING fts5(
						title, artist, album, folderName,
						content=songs, content_rowid=id
					)
					"""
				)
				db.execSQL(
					"""
					CREATE TRIGGER IF NOT EXISTS songs_ai AFTER INSERT ON songs BEGIN
						INSERT INTO songs_fts(rowid, title, artist, album, folderName)
						VALUES (new.id, new.title, new.artist, new.album, new.folderName);
					END
					"""
				)
				db.execSQL(
					"""
					CREATE TRIGGER IF NOT EXISTS songs_ad AFTER DELETE ON songs BEGIN
						INSERT INTO songs_fts(songs_fts, rowid, title, artist, album, folderName)
						VALUES ('delete', old.id, old.title, old.artist, old.album, old.folderName);
					END
					"""
				)
				db.execSQL(
					"""
					CREATE TRIGGER IF NOT EXISTS songs_au AFTER UPDATE ON songs BEGIN
						INSERT INTO songs_fts(songs_fts, rowid, title, artist, album, folderName)
						VALUES ('delete', old.id, old.title, old.artist, old.album, old.folderName);
						INSERT INTO songs_fts(rowid, title, artist, album, folderName)
						VALUES (new.id, new.title, new.artist, new.album, new.folderName);
					END
					"""
				)

				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS song_stats (
						songId INTEGER PRIMARY KEY NOT NULL,
						rating INTEGER NOT NULL DEFAULT 0,
						lastPositionMs INTEGER NOT NULL DEFAULT 0,
						audioTrackId TEXT DEFAULT NULL,
						FOREIGN KEY (songId) REFERENCES songs(id) ON DELETE CASCADE
					)
					"""
				)
				db.execSQL("CREATE INDEX IF NOT EXISTS index_song_stats_songId ON song_stats (songId)")

				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS music_queues (
						id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						name TEXT NOT NULL,
						createdAt INTEGER NOT NULL,
						isFavorite INTEGER NOT NULL DEFAULT 0,
						sourceType TEXT NOT NULL DEFAULT 'manual',
						sourceId TEXT DEFAULT NULL
					)
					"""
				)
				db.execSQL("CREATE INDEX IF NOT EXISTS index_music_queues_createdAt ON music_queues (createdAt)")

				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS music_queue_tracks (
						queueId INTEGER NOT NULL,
						songId INTEGER NOT NULL,
						position INTEGER NOT NULL,
						addedAt INTEGER NOT NULL,
						PRIMARY KEY(queueId, songId),
						FOREIGN KEY (queueId) REFERENCES music_queues(id) ON DELETE CASCADE,
						FOREIGN KEY (songId) REFERENCES songs(id) ON DELETE CASCADE
					)
					"""
				)
				db.execSQL("CREATE INDEX IF NOT EXISTS index_music_queue_tracks_queueId ON music_queue_tracks (queueId)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_music_queue_tracks_songId ON music_queue_tracks (songId)")

				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS scan_keep_song_ids (
						id INTEGER PRIMARY KEY NOT NULL
					)
					"""
				)
			}
		}
	}
}
