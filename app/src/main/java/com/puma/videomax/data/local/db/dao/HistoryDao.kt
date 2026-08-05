package com.puma.videomax.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.puma.videomax.data.local.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(entity: HistoryEntity)

	@Query("SELECT * FROM playback_history ORDER BY watchedAt DESC")
	fun observeAll(): Flow<List<HistoryEntity>>

	@Query("DELETE FROM playback_history")
	suspend fun clearAll()

	@Query("DELETE FROM playback_history WHERE videoId = :videoId")
	suspend fun delete(videoId: Long)
}
