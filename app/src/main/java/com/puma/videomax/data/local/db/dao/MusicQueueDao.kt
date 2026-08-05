package com.puma.videomax.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.puma.videomax.data.local.db.entity.MusicQueueEntity
import com.puma.videomax.data.local.db.entity.MusicQueueTrackEntity
import com.puma.videomax.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class QueueWithTrackCount(
	val id: Long,
	val name: String,
	val createdAt: Long,
	val isFavorite: Boolean,
	val trackCount: Int
)

@Dao
interface MusicQueueDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertQueue(queue: MusicQueueEntity): Long

	@Query("DELETE FROM music_queues WHERE id = :queueId")
	suspend fun deleteQueue(queueId: Long)

	@Query("UPDATE music_queues SET isFavorite = :isFavorite WHERE id = :queueId")
	suspend fun updateFavorite(queueId: Long, isFavorite: Boolean)

	@Query("UPDATE music_queues SET name = :name WHERE id = :queueId")
	suspend fun renameQueue(queueId: Long, name: String)

	@Query(
		"""
		SELECT mq.id, mq.name, mq.createdAt, mq.isFavorite, COUNT(mqt.songId) AS trackCount
		FROM music_queues mq
		LEFT JOIN music_queue_tracks mqt ON mq.id = mqt.queueId
		GROUP BY mq.id
		ORDER BY mq.createdAt DESC
		"""
	)
	fun observeQueues(): Flow<List<QueueWithTrackCount>>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addTrack(crossRef: MusicQueueTrackEntity)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addTracks(crossRefs: List<MusicQueueTrackEntity>)

	@Query("DELETE FROM music_queue_tracks WHERE queueId = :queueId")
	suspend fun clearQueueTracks(queueId: Long)

	@Query("DELETE FROM music_queue_tracks WHERE queueId = :queueId AND songId = :songId")
	suspend fun removeTrack(queueId: Long, songId: Long)

	@Query(
		"""
		SELECT s.* FROM songs s
		INNER JOIN music_queue_tracks mqt ON s.id = mqt.songId
		WHERE mqt.queueId = :queueId
		ORDER BY mqt.position ASC
		"""
	)
	fun observeSongs(queueId: Long): Flow<List<SongEntity>>

	@Query(
		"""
		SELECT s.* FROM songs s
		INNER JOIN music_queue_tracks mqt ON s.id = mqt.songId
		WHERE mqt.queueId = :queueId
		ORDER BY mqt.position ASC
		"""
	)
	suspend fun getSongs(queueId: Long): List<SongEntity>
}
