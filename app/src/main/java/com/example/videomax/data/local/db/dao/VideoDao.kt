package com.example.videomax.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

	@Query(
		"""
		SELECT * FROM videos
		WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%')
		ORDER BY
			CASE WHEN :sort = 'DATE_DESC' THEN dateAdded END DESC,
			CASE WHEN :sort = 'DATE_ASC' THEN dateAdded END ASC,
			CASE WHEN :sort = 'NAME_ASC' THEN displayName COLLATE NOCASE END ASC,
			CASE WHEN :sort = 'NAME_DESC' THEN displayName COLLATE NOCASE END DESC,
			CASE WHEN :sort = 'DURATION_DESC' THEN durationMs END DESC,
			CASE WHEN :sort = 'DURATION_ASC' THEN durationMs END ASC,
			CASE WHEN :sort = 'SIZE_DESC' THEN sizeBytes END DESC,
			CASE WHEN :sort = 'SIZE_ASC' THEN sizeBytes END ASC
		"""
	)
	fun pagingVideos(query: String, sort: String): PagingSource<Int, VideoEntity>

	@Query(
		"""
		SELECT * FROM videos
		WHERE folderName = :folder
		ORDER BY
			CASE WHEN :sort = 'DATE_DESC' THEN dateAdded END DESC,
			CASE WHEN :sort = 'DATE_ASC' THEN dateAdded END ASC,
			CASE WHEN :sort = 'NAME_ASC' THEN displayName COLLATE NOCASE END ASC,
			CASE WHEN :sort = 'NAME_DESC' THEN displayName COLLATE NOCASE END DESC,
			CASE WHEN :sort = 'DURATION_DESC' THEN durationMs END DESC,
			CASE WHEN :sort = 'DURATION_ASC' THEN durationMs END ASC,
			CASE WHEN :sort = 'SIZE_DESC' THEN sizeBytes END DESC,
			CASE WHEN :sort = 'SIZE_ASC' THEN sizeBytes END ASC
		"""
	)
	fun pagingByFolder(folder: String, sort: String): PagingSource<Int, VideoEntity>

	@Query(
		"""
		SELECT id FROM videos
		WHERE (:query = '' OR displayName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%')
		ORDER BY
			CASE WHEN :sort = 'DATE_DESC' THEN dateAdded END DESC,
			CASE WHEN :sort = 'DATE_ASC' THEN dateAdded END ASC,
			CASE WHEN :sort = 'NAME_ASC' THEN displayName COLLATE NOCASE END ASC,
			CASE WHEN :sort = 'NAME_DESC' THEN displayName COLLATE NOCASE END DESC,
			CASE WHEN :sort = 'DURATION_DESC' THEN durationMs END DESC,
			CASE WHEN :sort = 'DURATION_ASC' THEN durationMs END ASC,
			CASE WHEN :sort = 'SIZE_DESC' THEN sizeBytes END DESC,
			CASE WHEN :sort = 'SIZE_ASC' THEN sizeBytes END ASC
		"""
	)
	suspend fun getVideoIds(query: String, sort: String): List<Long>

	@Query(
		"""
		SELECT id FROM videos
		WHERE folderName = :folder
		ORDER BY
			CASE WHEN :sort = 'DATE_DESC' THEN dateAdded END DESC,
			CASE WHEN :sort = 'DATE_ASC' THEN dateAdded END ASC,
			CASE WHEN :sort = 'NAME_ASC' THEN displayName COLLATE NOCASE END ASC,
			CASE WHEN :sort = 'NAME_DESC' THEN displayName COLLATE NOCASE END DESC,
			CASE WHEN :sort = 'DURATION_DESC' THEN durationMs END DESC,
			CASE WHEN :sort = 'DURATION_ASC' THEN durationMs END ASC,
			CASE WHEN :sort = 'SIZE_DESC' THEN sizeBytes END DESC,
			CASE WHEN :sort = 'SIZE_ASC' THEN sizeBytes END ASC
		"""
	)
	suspend fun getVideoIdsByFolder(folder: String, sort: String): List<Long>

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

	@Query("SELECT id FROM videos")
	suspend fun getAllIds(): List<Long>

	@Query("DELETE FROM videos WHERE id IN (:ids)")
	suspend fun deleteByIds(ids: List<Long>)
}
