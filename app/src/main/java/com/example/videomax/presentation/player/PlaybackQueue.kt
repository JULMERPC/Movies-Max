package com.example.videomax.presentation.player

import com.example.videomax.domain.model.SortOption
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
 */
@Singleton
class PlaybackQueue @Inject constructor() {

	@Volatile
	private var videoIds: MutableList<Long> = mutableListOf()

	@Volatile
	private var index: Int = 0

	@Volatile
	private var lazySession: Boolean = false

	@Volatile
	private var queueContext: PlaybackQueueContext? = null

	@Volatile
	private var hasMoreBefore: Boolean = false

	@Volatile
	private var hasMoreAfter: Boolean = false

	fun setQueue(ids: List<Long>, startId: Long) {
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
	fun beginLazy(context: PlaybackQueueContext, startId: Long) {
		lazySession = true
		queueContext = context
		videoIds = mutableListOf(startId)
		index = 0
		// Optimistic until the first window query resolves.
		hasMoreBefore = true
		hasMoreAfter = true
	}

	fun isLazy(): Boolean = lazySession

	fun context(): PlaybackQueueContext? = queueContext

	fun hasMoreBefore(): Boolean = hasMoreBefore

	fun hasMoreAfter(): Boolean = hasMoreAfter

	/**
	 * Replaces the in-memory window after the first Room fetch around [startId].
	 */
	fun applyWindow(
		windowIds: List<Long>,
		startId: Long,
		moreBefore: Boolean,
		moreAfter: Boolean
	) {
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
	fun prepend(ids: List<Long>, moreBefore: Boolean): Int {
		val fresh = ids.distinct().filter { it !in videoIds }
		if (fresh.isNotEmpty()) {
			videoIds.addAll(0, fresh)
			index += fresh.size
		}
		hasMoreBefore = moreBefore
		return fresh.size
	}

	/** Appends later-in-navigation items (appear later in sort order). */
	fun append(ids: List<Long>, moreAfter: Boolean): Int {
		val fresh = ids.distinct().filter { it !in videoIds }
		if (fresh.isNotEmpty()) {
			videoIds.addAll(fresh)
		}
		hasMoreAfter = moreAfter
		return fresh.size
	}

	fun needsExpandBefore(threshold: Int = EXPAND_THRESHOLD): Boolean =
		lazySession && hasMoreBefore && index < threshold

	fun needsExpandAfter(threshold: Int = EXPAND_THRESHOLD): Boolean =
		lazySession && hasMoreAfter && (videoIds.size - index - 1) < threshold

	fun setIndex(idx: Int) {
		if (idx in videoIds.indices) {
			index = idx
		}
	}

	fun ensureSingle(videoId: Long) {
		if (videoIds.isEmpty() || (currentId() != videoId && videoId !in videoIds)) {
			if (lazySession && queueContext != null) {
				beginLazy(queueContext!!, videoId)
			} else {
				setQueue(listOf(videoId), videoId)
			}
		} else if (currentId() != videoId) {
			val found = videoIds.indexOf(videoId)
			if (found >= 0) index = found
		}
	}

	fun currentId(): Long? = videoIds.getOrNull(index)

	fun size(): Int = videoIds.size

	fun currentIndex(): Int = index

	fun hasPrevious(): Boolean = index > 0 || (lazySession && hasMoreBefore)

	fun hasNext(): Boolean = index < videoIds.lastIndex || (lazySession && hasMoreAfter)

	fun moveToPrevious(): Long? {
		if (index <= 0) return null
		index -= 1
		return currentId()
	}

	fun moveToNext(): Long? {
		if (index >= videoIds.lastIndex) return null
		index += 1
		return currentId()
	}

	fun moveToFirst(): Long? {
		if (videoIds.isEmpty()) return null
		index = 0
		return currentId()
	}

	fun ids(): List<Long> = videoIds.toList()

	fun firstId(): Long? = videoIds.firstOrNull()

	fun lastId(): Long? = videoIds.lastOrNull()

	companion object {
		const val WINDOW_BEFORE = 12
		const val WINDOW_AFTER = 12
		const val EXPAND_CHUNK = 16
		const val EXPAND_THRESHOLD = 4
	}
}
