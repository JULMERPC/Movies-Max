package com.puma.videomax.presentation.player

import com.puma.videomax.domain.model.SortOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context that defines the ordered universe of a lazy library queue
 * (same filters/sort/folder as the Library list the user was browsing).
 */
data class PlaybackQueueContext(
	val query: String = "",
	val sortOption: SortOption = SortOption.DATE_DESC,
	val folder: String? = null
)

/**
 * In-memory playback queue for the current player session.
 *
 * Supports:
 * - **Eager** queues ([setQueue]): playlists, favorites, history — full ID list known up front.
 * - **Lazy** queues ([beginLazy]): library — starts with the selected ID and expands a window
 *   around the playhead on demand, matching professional players (VLC / MX / Nova).
 *
 * Thread-safety: all mutable state is guarded by [lock] to prevent race conditions
 * when concurrent coroutines expand the window or navigate the queue.
 */
@Singleton
class PlaybackQueue @Inject constructor() {

	private val lock = Any()
	private var videoIds: MutableList<Long> = mutableListOf()
	private var index: Int = 0
	private var lazySession: Boolean = false
	private var queueContext: PlaybackQueueContext? = null
	private var hasMoreBefore: Boolean = false
	private var hasMoreAfter: Boolean = false

	fun setQueue(ids: List<Long>, startId: Long) = synchronized(lock) {
		val distinct = ids.distinct()
		lazySession = false
		queueContext = null
		hasMoreBefore = false
		hasMoreAfter = false
		videoIds = if (distinct.isEmpty()) {
			mutableListOf(startId)
		} else {
			distinct.toMutableList()
		}
		index = videoIds.indexOf(startId).takeIf { it >= 0 } ?: 0
	}

	/**
	 * Starts a lazy library session with only the selected video.
	 * The player must expand the window asynchronously.
	 */
	fun beginLazy(context: PlaybackQueueContext, startId: Long) = synchronized(lock) {
		lazySession = true
		queueContext = context
		videoIds = mutableListOf(startId)
		index = 0
		hasMoreBefore = true
		hasMoreAfter = true
	}

	fun isLazy(): Boolean = synchronized(lock) { lazySession }

	fun context(): PlaybackQueueContext? = synchronized(lock) { queueContext }

	fun hasMoreBefore(): Boolean = synchronized(lock) { hasMoreBefore }

	fun hasMoreAfter(): Boolean = synchronized(lock) { hasMoreAfter }

	/**
	 * Replaces the in-memory window after the first Room fetch around [startId].
	 */
	fun applyWindow(
		windowIds: List<Long>,
		startId: Long,
		moreBefore: Boolean,
		moreAfter: Boolean
	) = synchronized(lock) {
		val distinct = windowIds.distinct()
		if (distinct.isEmpty()) {
			videoIds = mutableListOf(startId)
			index = 0
		} else {
			videoIds = distinct.toMutableList()
			index = videoIds.indexOf(startId).takeIf { it >= 0 } ?: 0
		}
		hasMoreBefore = moreBefore
		hasMoreAfter = moreAfter
	}

	/** Prepends older-in-navigation items (appear earlier in sort order). */
	fun prepend(ids: List<Long>, moreBefore: Boolean): Int = synchronized(lock) {
		val fresh = ids.distinct().filter { it !in videoIds }
		if (fresh.isNotEmpty()) {
			videoIds.addAll(0, fresh)
			index += fresh.size
		}
		hasMoreBefore = moreBefore
		fresh.size
	}

	/** Appends later-in-navigation items (appear later in sort order). */
	fun append(ids: List<Long>, moreAfter: Boolean): Int = synchronized(lock) {
		val fresh = ids.distinct().filter { it !in videoIds }
		if (fresh.isNotEmpty()) {
			videoIds.addAll(fresh)
		}
		hasMoreAfter = moreAfter
		fresh.size
	}

	fun needsExpandBefore(threshold: Int = EXPAND_THRESHOLD): Boolean = synchronized(lock) {
		lazySession && hasMoreBefore && index < threshold
	}

	fun needsExpandAfter(threshold: Int = EXPAND_THRESHOLD): Boolean = synchronized(lock) {
		lazySession && hasMoreAfter && (videoIds.size - index - 1) < threshold
	}

	fun setIndex(idx: Int) = synchronized(lock) {
		if (idx in videoIds.indices) {
			index = idx
		}
	}

	fun ensureSingle(videoId: Long) = synchronized(lock) {
		if (videoIds.isEmpty() || (currentIdLocked() != videoId && videoId !in videoIds)) {
			if (lazySession && queueContext != null) {
				beginLazy(queueContext!!, videoId)
			} else {
				setQueue(listOf(videoId), videoId)
			}
		} else if (currentIdLocked() != videoId) {
			val found = videoIds.indexOf(videoId)
			if (found >= 0) index = found
		}
	}

	fun currentId(): Long? = synchronized(lock) { currentIdLocked() }

	private fun currentIdLocked(): Long? = videoIds.getOrNull(index)

	fun size(): Int = synchronized(lock) { videoIds.size }

	fun currentIndex(): Int = synchronized(lock) { index }

	fun hasPrevious(): Boolean = synchronized(lock) { index > 0 || (lazySession && hasMoreBefore) }

	fun hasNext(): Boolean = synchronized(lock) { index < videoIds.lastIndex || (lazySession && hasMoreAfter) }

	fun moveToPrevious(): Long? = synchronized(lock) {
		if (index <= 0) return null
		index -= 1
		currentIdLocked()
	}

	fun moveToNext(): Long? = synchronized(lock) {
		if (index >= videoIds.lastIndex) return null
		index += 1
		currentIdLocked()
	}

	fun moveToFirst(): Long? = synchronized(lock) {
		if (videoIds.isEmpty()) return null
		index = 0
		currentIdLocked()
	}

	fun ids(): List<Long> = synchronized(lock) { videoIds.toList() }

	fun firstId(): Long? = synchronized(lock) { videoIds.firstOrNull() }

	fun lastId(): Long? = synchronized(lock) { videoIds.lastOrNull() }

	companion object {
		const val WINDOW_BEFORE = 12
		const val WINDOW_AFTER = 12
		const val EXPAND_CHUNK = 16
		const val EXPAND_THRESHOLD = 4
	}
}
