package com.puma.videomax.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.puma.videomax.data.local.db.entity.SongStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongStatsDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(stats: SongStatsEntity)

	@Query("SELECT * FROM song_stats WHERE songId = :songId LIMIT 1")
	suspend fun getBySongId(songId: Long): SongStatsEntity?

	@Query("SELECT * FROM song_stats WHERE songId = :songId LIMIT 1")
	fun observeBySongId(songId: Long): Flow<SongStatsEntity?>

	@Query("UPDATE song_stats SET lastPositionMs = :positionMs WHERE songId = :songId")
	suspend fun updateLastPosition(songId: Long, positionMs: Long)

	@Query("UPDATE song_stats SET rating = :rating WHERE songId = :songId")
	suspend fun updateRating(songId: Long, rating: Int)

	@Query("UPDATE song_stats SET audioTrackId = :trackId WHERE songId = :songId")
	suspend fun updateAudioTrack(songId: Long, trackId: String?)

	@Query("DELETE FROM song_stats WHERE songId = :songId")
	suspend fun delete(songId: Long)

	@Query("DELETE FROM song_stats")
	suspend fun clearAll()
}
