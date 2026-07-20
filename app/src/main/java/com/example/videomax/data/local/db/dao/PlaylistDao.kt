package com.example.videomax.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.videomax.data.local.db.entity.PlaylistEntity
import com.example.videomax.data.local.db.entity.PlaylistVideoCrossRef
import com.example.videomax.data.local.db.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
	val id: Long,
	val name: String,
	val createdAt: Long,
	val videoCount: Int
)

@Dao
interface PlaylistDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertPlaylist(playlist: PlaylistEntity): Long

	@Query("DELETE FROM playlists WHERE id = :playlistId")
	suspend fun deletePlaylist(playlistId: Long)

	@Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
	suspend fun renamePlaylist(playlistId: Long, name: String)

	@Query(
		"""
		SELECT p.id, p.name, p.createdAt, COUNT(pv.videoId) AS videoCount
		FROM playlists p
		LEFT JOIN playlist_videos pv ON p.id = pv.playlistId
		GROUP BY p.id
		ORDER BY p.createdAt DESC
		"""
	)
	fun observePlaylists(): Flow<List<PlaylistWithCount>>

	@Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
	fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addVideo(crossRef: PlaylistVideoCrossRef)

	@Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
	suspend fun removeVideo(playlistId: Long, videoId: Long)

	@Query(
		"""
		SELECT v.* FROM videos v
		INNER JOIN playlist_videos pv ON v.id = pv.videoId
		WHERE pv.playlistId = :playlistId
		ORDER BY pv.addedAt ASC
		"""
	)
	fun observeVideos(playlistId: Long): Flow<List<VideoEntity>>

	@Query(
		"""
		SELECT v.* FROM videos v
		INNER JOIN playlist_videos pv ON v.id = pv.videoId
		WHERE pv.playlistId = :playlistId
		ORDER BY pv.addedAt ASC
		"""
	)
	suspend fun getVideos(playlistId: Long): List<VideoEntity>
}
