package com.example.videomax.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.videomax.data.local.db.entity.ScanKeepIdEntity
import com.example.videomax.data.local.db.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class VideoUserState(
	val id: Long,
	val isFavorite: Boolean,
	val lastPositionMs: Long,
	val playCount: Int
)

@Dao
interface VideoDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsertAll(videos: List<VideoEntity>)

	@Query("DELETE FROM videos")
	suspend fun clearAll()

	@Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): VideoEntity?

	@Query("SELECT id, isFavorite, lastPositionMs, playCount FROM videos")
	suspend fun getUserStates(): List<VideoUserState>

	@Query("SELECT id, isFavorite, lastPositionMs, playCount FROM videos WHERE id IN (:ids)")
	suspend fun getUserStatesForIds(ids: List<Long>): List<VideoUserState>

	@Query("SELECT * FROM videos WHERE id IN (:ids)")
	suspend fun getByIds(ids: List<Long>): List<VideoEntity>

	// --- pagingVideos ---
	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY dateAdded DESC")
	fun pagingVideosDateDesc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY dateAdded ASC")
	fun pagingVideosDateAsc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY displayName COLLATE NOCASE ASC")
	fun pagingVideosNameAsc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY displayName COLLATE NOCASE DESC")
	fun pagingVideosNameDesc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY durationMs DESC")
	fun pagingVideosDurationDesc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY durationMs ASC")
	fun pagingVideosDurationAsc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY sizeBytes DESC")
	fun pagingVideosSizeDesc(query: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY sizeBytes ASC")
	fun pagingVideosSizeAsc(query: String): PagingSource<Int, VideoEntity>

	// --- pagingByFolder ---
	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY dateAdded DESC")
	fun pagingByFolderDateDesc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY dateAdded ASC")
	fun pagingByFolderDateAsc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY displayName COLLATE NOCASE ASC")
	fun pagingByFolderNameAsc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY displayName COLLATE NOCASE DESC")
	fun pagingByFolderNameDesc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY durationMs DESC")
	fun pagingByFolderDurationDesc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY durationMs ASC")
	fun pagingByFolderDurationAsc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY sizeBytes DESC")
	fun pagingByFolderSizeDesc(folder: String): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE folderName = :folder ORDER BY sizeBytes ASC")
	fun pagingByFolderSizeAsc(folder: String): PagingSource<Int, VideoEntity>

	// --- getVideoIds ---
	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY dateAdded DESC")
	suspend fun getVideoIdsDateDesc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY dateAdded ASC")
	suspend fun getVideoIdsDateAsc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY displayName COLLATE NOCASE ASC")
	suspend fun getVideoIdsNameAsc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY displayName COLLATE NOCASE DESC")
	suspend fun getVideoIdsNameDesc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY durationMs DESC")
	suspend fun getVideoIdsDurationDesc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY durationMs ASC")
	suspend fun getVideoIdsDurationAsc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY sizeBytes DESC")
	suspend fun getVideoIdsSizeDesc(query: String): List<Long>

	@Query("SELECT id FROM videos WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%') ORDER BY sizeBytes ASC")
	suspend fun getVideoIdsSizeAsc(query: String): List<Long>

	// --- getVideoIdsByFolder ---
	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY dateAdded DESC")
	suspend fun getVideoIdsByFolderDateDesc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY dateAdded ASC")
	suspend fun getVideoIdsByFolderDateAsc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY displayName COLLATE NOCASE ASC")
	suspend fun getVideoIdsByFolderNameAsc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY displayName COLLATE NOCASE DESC")
	suspend fun getVideoIdsByFolderNameDesc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY durationMs DESC")
	suspend fun getVideoIdsByFolderDurationDesc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY durationMs ASC")
	suspend fun getVideoIdsByFolderDurationAsc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY sizeBytes DESC")
	suspend fun getVideoIdsByFolderSizeDesc(folder: String): List<Long>

	@Query("SELECT id FROM videos WHERE folderName = :folder ORDER BY sizeBytes ASC")
	suspend fun getVideoIdsByFolderSizeAsc(folder: String): List<Long>

	@Query("SELECT DISTINCT folderName FROM videos ORDER BY folderName COLLATE NOCASE ASC")
	fun observeFolders(): Flow<List<String>>

	@Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY displayName COLLATE NOCASE ASC")
	fun observeFavorites(): Flow<List<VideoEntity>>

	@Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY displayName COLLATE NOCASE ASC")
	fun pagingFavorites(): PagingSource<Int, VideoEntity>

	@Query("SELECT * FROM videos WHERE playCount > 0 ORDER BY playCount DESC, dateModified DESC LIMIT :limit")
	fun observeMostPlayed(limit: Int = 100): Flow<List<VideoEntity>>

	@Query("SELECT * FROM videos WHERE playCount > 0 ORDER BY playCount DESC, dateModified DESC")
	fun pagingMostPlayed(): PagingSource<Int, VideoEntity>

	@Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :videoId")
	suspend fun updateFavorite(videoId: Long, isFavorite: Boolean)

	@Query("SELECT isFavorite FROM videos WHERE id = :videoId LIMIT 1")
	suspend fun isFavorite(videoId: Long): Boolean?

	@Query("UPDATE videos SET lastPositionMs = :positionMs WHERE id = :videoId")
	suspend fun updateLastPosition(videoId: Long, positionMs: Long)

	@Query("UPDATE videos SET playCount = playCount + 1 WHERE id = :videoId")
	suspend fun incrementPlayCount(videoId: Long)

	@Query("SELECT COUNT(*) FROM videos")
	fun observeCount(): Flow<Int>

	@Query("SELECT COUNT(*) FROM videos")
	suspend fun count(): Int

	@Query("DELETE FROM videos WHERE id IN (:ids)")
	suspend fun deleteByIds(ids: List<Long>)

	// --- Scan keep-set (stale sync without loading all IDs into memory) ---

	@Query("DELETE FROM scan_keep_ids")
	suspend fun clearScanKeepIds()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertScanKeepIds(ids: List<ScanKeepIdEntity>)

	/**
	 * Removes Room videos that were not seen in the current MediaStore scan.
	 * Runs entirely in SQLite — no full ID list materialization in the JVM.
	 */
	@Query("DELETE FROM videos WHERE id NOT IN (SELECT id FROM scan_keep_ids)")
	suspend fun deleteVideosNotInScanKeepSet()

	/** Keyset / page queries for lazy playback queues. */
	@RawQuery
	suspend fun queryIds(query: SupportSQLiteQuery): List<Long>
}
