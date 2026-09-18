package com.puma.videomax.service.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplementary noisy-receiver only. AudioFocus ownership is EXCLUSIVE to
 * Media3 (`setHandleAudioFocus=true` in the service player): it requests focus
 * once on `play()` and pauses/ducks/resumes internally on LOSS/TRANSIENT/GAIN.
 *
 * A previous version implemented `OnAudioFocusChangeListener` here AND wrote
 * `player.volume` on duck — two focus owners fighting over one player. Any
 * repeated transient event (notification, BT SCO, assistant) re-drove
 * `play()`/`pause()` from two paths and flickered into a millisecond
 * play/pause loop. Never request focus or write player state from here;
 * `onAudioFocusChange` callbacks must never call `play()`/`prepare()`.
 */
@Singleton
class AudioFocusHandler @Inject constructor(
	@ApplicationContext private val context: Context,
) {

	var onPause: () -> Unit = {}
	var onNoisy: () -> Unit = {}

	private val noisyReceiver = object : BroadcastReceiver() {
		override fun onReceive(c: Context?, intent: Intent?) {
			if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onNoisy()
		}
	}

	private var attached = false

	fun attach() {
		if (attached) return
		attached = true
		context.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
	}

	fun detach() {
		if (!attached) return
		attached = false
		runCatching { context.unregisterReceiver(noisyReceiver) }
	}
}
