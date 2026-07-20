package com.example.videomax.presentation.player

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory playback queue for the current player session.
 * Screens set the queue from the visible list before navigating to the player.
 */
@Singleton
class PlaybackQueue @Inject constructor() {

	@Volatile
	private var videoIds: List<Long> = emptyList()

	@Volatile
	private var index: Int = 0

	fun setQueue(ids: List<Long>, startId: Long) {
		val distinct = ids.distinct()
		videoIds = if (distinct.isEmpty()) listOf(startId) else distinct
		index = videoIds.indexOf(startId).takeIf { it >= 0 } ?: 0
	}

	fun ensureSingle(videoId: Long) {
		if (videoIds.isEmpty() || currentId() != videoId && videoId !in videoIds) {
			setQueue(listOf(videoId), videoId)
		} else if (currentId() != videoId) {
			val found = videoIds.indexOf(videoId)
			if (found >= 0) index = found
		}
	}

	fun currentId(): Long? = videoIds.getOrNull(index)

	fun size(): Int = videoIds.size

	fun currentIndex(): Int = index

	fun hasPrevious(): Boolean = index > 0

	fun hasNext(): Boolean = index < videoIds.lastIndex

	fun moveToPrevious(): Long? {
		if (!hasPrevious()) return null
		index -= 1
		return currentId()
	}

	fun moveToNext(): Long? {
		if (!hasNext()) return null
		index += 1
		return currentId()
	}

	fun moveToFirst(): Long? {
		if (videoIds.isEmpty()) return null
		index = 0
		return currentId()
	}

	fun ids(): List<Long> = videoIds
}
