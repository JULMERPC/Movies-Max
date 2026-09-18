package com.puma.videomax.service.audio

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactive STREAM_MUSIC volume (Namida pattern: observe, never poll).
 *
 * Spam fix: the old version observed [Settings.System.CONTENT_URI] with
 * `notifyForDescendants=true`, which fires for EVERY system-setting change
 * (brightness, etc.). Each fire cost a `getStreamVolume()` IPC to
 * `system_server` — dozens per second in Logcat. This version:
 *
 * 1. Observes ONLY the `volume_music*` URIs, with descendants=false.
 * 2. Debounces bursts (volume-button repeats) to one read per 150 ms AND
 *    enforces a minimum IPC interval by timestamp, so no path can fire two
 *    consecutive `getStreamVolume()` IPCs to `system_server`.
 * 3. Emits only when the numeric level actually changed (StateFlow is already
 *    distinct; the explicit compare also skips the IPC result handling).
 * 4. Caches `max()` — it never changes at runtime, so [fraction()] is IPC-free.
 * 5. Every AudioManager call is wrapped in `runCatching`.
 */
@Singleton
class VolumeStateProvider @Inject constructor(
	@ApplicationContext context: Context,
) {
	private val app = context.applicationContext
	private val audioManager: AudioManager = app.getSystemService(AudioManager::class.java)

	private val _volume = MutableStateFlow(readVolume() ?: 0)
	val volume: StateFlow<Int> = _volume.asStateFlow()

	@Volatile
	private var maxCached: Int = readMax()

	// Narrow URIs: music stream + its routed variants (BT A2DP/HFP, USB).
	private val volumeUris: List<Uri> = listOf(
		"volume_music",
		"volume_music_bt_a2dp",
		"volume_music_bt_hfp",
		"volume_music_usb"
	).map { Settings.System.getUriFor(it) }

	private val debounceHandler = Handler(Looper.getMainLooper())
	private val debounceMs = 150L
	// Explicit timestamp throttle (req 1): no two getStreamVolume IPCs closer
	// than this, regardless of which path scheduled the read.
	private val minIpcIntervalMs = 100L
	@Volatile
	private var lastIpcMs = 0L
	private val debouncedRefresh: Runnable = Runnable {
		val now = android.os.SystemClock.uptimeMillis()
		val elapsed = now - lastIpcMs
		if (elapsed < minIpcIntervalMs) {
			// Too soon: re-post only the remainder instead of hitting IPC now.
			debounceHandler.removeCallbacks(debouncedRefresh)
			debounceHandler.postDelayed(debouncedRefresh, minIpcIntervalMs - elapsed)
			return@Runnable
		}
		lastIpcMs = now
		val fresh = readVolume() ?: return@Runnable
		if (fresh != _volume.value) _volume.value = fresh
	}

	private val observer = object : ContentObserver(debounceHandler) {
		override fun onChange(selfChange: Boolean, uri: Uri?) = scheduleRefresh()
		override fun onChange(selfChange: Boolean) = scheduleRefresh()
	}

	private var started = false

	private fun scheduleRefresh() {
		if (!started) return
		debounceHandler.removeCallbacks(debouncedRefresh)
		debounceHandler.postDelayed(debouncedRefresh, debounceMs)
	}

	fun start() {
		if (started) return
		started = true
		maxCached = readMax()
		readVolume()?.let { fresh ->
			if (fresh != _volume.value) _volume.value = fresh
		}
		volumeUris.forEach { uri ->
			app.contentResolver.registerContentObserver(uri, false, observer)
		}
	}

	fun stop() {
		if (!started) return
		started = false
		debounceHandler.removeCallbacks(debouncedRefresh)
		runCatching { app.contentResolver.unregisterContentObserver(observer) }
	}

	/** Cached: no IPC. Refresh via [start] (re-reads max). */
	fun max(): Int = maxCached

	/** IPC-free: derived from the cached StateFlow value + cached max. */
	fun fraction(): Float = _volume.value / maxCached.coerceAtLeast(1).toFloat()

	private fun readVolume(): Int? = runCatching {
		audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
	}.getOrNull()

	private fun readMax(): Int = runCatching {
		audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
	}.getOrDefault(15).coerceAtLeast(1)
}
