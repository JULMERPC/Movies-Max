package com.puma.videomax.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class RepeatMode { OFF, ONE, ALL }

data class AudioQueueItem(
	val videoId: Long,
	val uri: String,
	val displayName: String,
	val artist: String = "",
	val album: String = "",
	val albumId: Long = -1L
)

object BackgroundAudioManager {

	private val _queue = MutableStateFlow<List<AudioQueueItem>>(emptyList())
	val queue: StateFlow<List<AudioQueueItem>> = _queue.asStateFlow()

	private val _currentIndex = MutableStateFlow(-1)
	val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

	private val _isPlaying = MutableStateFlow(false)
	val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

	private val _currentPosition = MutableStateFlow(0L)
	val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

	private val _duration = MutableStateFlow(0L)
	val duration: StateFlow<Long> = _duration.asStateFlow()

	private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
	val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

	private val _isShuffleEnabled = MutableStateFlow(false)
	val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

	private var autoPlayNext = false
	private var onTrackChange: ((AudioQueueItem) -> Unit)? = null
	private var onPlayPauseChange: ((Boolean) -> Unit)? = null

	fun registerCallbacks(
		onTrackChange: (AudioQueueItem) -> Unit,
		onPlayPauseChange: (Boolean) -> Unit
	) {
		this.onTrackChange = onTrackChange
		this.onPlayPauseChange = onPlayPauseChange
	}

	fun unregisterCallbacks() {
		onTrackChange = null
		onPlayPauseChange = null
	}

	fun current(): AudioQueueItem? {
		val idx = _currentIndex.value
		val q = _queue.value
		return if (idx in q.indices) q[idx] else null
	}

	fun setQueue(items: List<AudioQueueItem>, startIndex: Int = 0, autoAdvance: Boolean = false) {
		autoPlayNext = autoAdvance
		_queue.value = items
		_currentIndex.value = startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
		_isPlaying.value = true
	}

	fun playQueue(items: List<AudioQueueItem>, startIndex: Int = 0, autoAdvance: Boolean = false) {
		setQueue(items, startIndex, autoAdvance)
		val item = current() ?: return
		onTrackChange?.invoke(item)
	}

	fun addToQueue(item: AudioQueueItem) {
		_queue.update { it + item }
	}

	fun setPlaying(playing: Boolean) {
		_isPlaying.value = playing
		onPlayPauseChange?.invoke(playing)
	}

	fun setPosition(ms: Long) {
		_currentPosition.value = ms
	}

	fun setDuration(ms: Long) {
		_duration.value = ms
	}

	fun cycleRepeatMode() {
		_repeatMode.value = when (_repeatMode.value) {
			RepeatMode.OFF -> RepeatMode.ALL
			RepeatMode.ALL -> RepeatMode.ONE
			RepeatMode.ONE -> RepeatMode.OFF
		}
	}

	fun toggleShuffle() {
		_isShuffleEnabled.value = !_isShuffleEnabled.value
		if (_isShuffleEnabled.value) {
			shuffleQueue()
		}
	}

	private fun shuffleQueue() {
		val q = _queue.value.toMutableList()
		if (q.size <= 1) return
		val currentItem = current() ?: return
		q.removeAt(_currentIndex.value)
		q.shuffle(Random)
		q.add(0, currentItem)
		_queue.value = q
		_currentIndex.value = 0
	}

	fun next(): AudioQueueItem? {
		val q = _queue.value
		if (q.isEmpty()) return null

		val repeat = _repeatMode.value
		if (repeat == RepeatMode.ONE) {
			return current()
		}

		val nextIdx = _currentIndex.value + 1
		return if (nextIdx in q.indices) {
			_currentIndex.value = nextIdx
			q[nextIdx]
		} else if (repeat == RepeatMode.ALL) {
			_currentIndex.value = 0
			q[0]
		} else {
			null
		}
	}

	fun previous(): AudioQueueItem? {
		val q = _queue.value
		if (q.isEmpty()) return null

		val prevIdx = _currentIndex.value - 1
		return if (prevIdx in q.indices) {
			_currentIndex.value = prevIdx
			q[prevIdx]
		} else {
			val lastIndex = q.lastIndex
			_currentIndex.value = lastIndex
			q[lastIndex]
		}
	}

	fun userNext() {
		val item = next() ?: return
		_isPlaying.value = true
		onTrackChange?.invoke(item)
	}

	fun userPrevious() {
		val item = previous() ?: return
		_isPlaying.value = true
		onTrackChange?.invoke(item)
	}

	fun seekTo(index: Int): AudioQueueItem? {
		val q = _queue.value
		return if (index in q.indices) {
			_currentIndex.value = index
			_isPlaying.value = true
			q[index]
		} else null
	}

	fun stop() {
		_isPlaying.value = false
		_currentPosition.value = 0L
		_duration.value = 0L
	}

	fun clear() {
		_queue.value = emptyList()
		_currentIndex.value = -1
		_isPlaying.value = false
		_currentPosition.value = 0L
		_duration.value = 0L
		_repeatMode.value = RepeatMode.OFF
		_isShuffleEnabled.value = false
	}

	fun removeFromQueue(index: Int) {
		_queue.update { q ->
			if (index !in q.indices) return@update q
			val newList = q.toMutableList().apply { removeAt(index) }
			if (_currentIndex.value >= newList.size) {
				_currentIndex.value = newList.lastIndex
			}
			newList
		}
	}
}
