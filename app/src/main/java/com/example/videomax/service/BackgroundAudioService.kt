package com.example.videomax.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.videomax.MainActivity
import com.example.videomax.R

class BackgroundAudioService : MediaSessionService() {

	private var mediaSession: MediaSession? = null

	@OptIn(UnstableApi::class)
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)

		val videoUri = intent?.getStringExtra(EXTRA_VIDEO_URI)
		val videoName = intent?.getStringExtra(EXTRA_VIDEO_NAME) ?: "Reproduciendo"

		if (videoUri == null) {
			stopSelf()
			return START_NOT_STICKY
		}

		if (mediaSession == null) {
			val player = ExoPlayer.Builder(this).build().apply {
				playWhenReady = true
			}

			val mediaItem = MediaItem.Builder()
				.setUri(Uri.parse(videoUri))
				.setMediaMetadata(
					MediaMetadata.Builder()
						.setTitle(videoName)
						.setArtist(getString(R.string.app_name))
						.build()
				)
				.build()

			player.setMediaItems(listOf(mediaItem))
			player.prepare()

			mediaSession = MediaSession.Builder(this, player)
				.setSessionActivity(buildBackStackIntent())
				.build()
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Audio en segundo plano",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Reproducción de audio en segundo plano"
			}
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(channel)
		}

		startForeground(NOTIFICATION_ID, buildNotification(videoName))

		return START_STICKY
	}

	private fun buildNotification(title: String): Notification {
		val pendingIntent = PendingIntent.getActivity(
			this, 0,
			Intent(this, MainActivity::class.java),
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)

		val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Notification.Builder(this, CHANNEL_ID)
		} else {
			@Suppress("DEPRECATION")
			Notification.Builder(this)
		}

		return builder
			.setContentTitle(title)
			.setContentText(getString(R.string.app_name))
			.setSmallIcon(android.R.drawable.ic_media_play)
			.setContentIntent(pendingIntent)
			.setOngoing(true)
			.build()
	}

	private fun buildBackStackIntent(): PendingIntent {
		val intent = Intent(this, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		return PendingIntent.getActivity(
			this, 0, intent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
		return mediaSession
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		val session = mediaSession
		if (session?.player?.playWhenReady == true) {
			session.player.playWhenReady = false
		}
		releaseSession()
		stopSelf()
	}

	private fun releaseSession() {
		mediaSession?.run {
			player.release()
			release()
		}
		mediaSession = null
	}

	override fun onDestroy() {
		releaseSession()
		super.onDestroy()
	}

	companion object {
		const val EXTRA_VIDEO_URI = "video_uri"
		const val EXTRA_VIDEO_NAME = "video_name"
		private const val CHANNEL_ID = "background_audio"
		private const val NOTIFICATION_ID = 1001
	}
}
