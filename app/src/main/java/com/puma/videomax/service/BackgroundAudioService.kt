package com.puma.videomax.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.puma.videomax.MainActivity

class BackgroundAudioService : MediaSessionService() {

	private var mediaSession: MediaSession? = null
	private val mainHandler = Handler(Looper.getMainLooper())

	override fun onCreate() {
		super.onCreate()
		createInvisibleChannel()
		val player = ExoPlayer.Builder(this).build().apply {
			setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(C.USAGE_MEDIA)
					.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
					.build(),
				/* handleAudioFocus = */ true
			)
			setHandleAudioBecomingNoisy(true)
			addListener(playbackListener)
			playWhenReady = true
		}
		mediaSession = MediaSession.Builder(this, player)
			.setSessionActivity(buildSessionActivity())
			.build()

		BackgroundAudioManager.registerCallbacks(
			onTrackChange = { item -> playTrack(item) },
			onPlayPauseChange = { playing ->
				mediaSession?.player?.let { player ->
					player.playWhenReady = playing
				}
			}
		)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		val session = mediaSession ?: return START_NOT_STICKY
		val player = session.player

		val autoAdvance = intent?.getBooleanExtra(EXTRA_AUTO_PLAY_NEXT, false) ?: false

		val videoUri = intent?.getStringExtra(EXTRA_VIDEO_URI)
		val videoName = intent?.getStringExtra(EXTRA_VIDEO_NAME) ?: "Reproduciendo"

		val existingQueue = BackgroundAudioManager.queue.value
		val hasManagerQueue = existingQueue.isNotEmpty() && BackgroundAudioManager.current() != null

		if (hasManagerQueue && videoUri.isNullOrBlank()) {
			val item = BackgroundAudioManager.current()!!
			player.setMediaItem(buildMediaItem(item))
			player.prepare()
			player.play()
			startForeground(NOTIFICATION_ID, buildNotification())
			return START_STICKY
		}

		if (videoUri.isNullOrBlank()) {
			if (player.mediaItemCount == 0) {
				stopSelf()
				return START_NOT_STICKY
			}
			return START_STICKY
		}

		val item = AudioQueueItem(
			videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0L),
			uri = videoUri,
			displayName = videoName
		)
		BackgroundAudioManager.setQueue(
			items = listOf(item),
			startIndex = 0,
			autoAdvance = autoAdvance
		)

		player.setMediaItem(buildMediaItem(item))
		player.prepare()
		player.play()

		startForeground(NOTIFICATION_ID, buildNotification())
		return START_STICKY
	}

	private fun playTrack(item: AudioQueueItem) {
		val player = mediaSession?.player ?: return
		player.setMediaItem(buildMediaItem(item))
		player.prepare()
		player.play()
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
		return mediaSession
	}

	private val playbackListener = object : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			val player = mediaSession?.player ?: return
			when (playbackState) {
				Player.STATE_READY -> {
					BackgroundAudioManager.setDuration(player.duration.coerceAtLeast(0L))
				}
				Player.STATE_ENDED -> {
					BackgroundAudioManager.setPosition(0L)
					val nextItem = BackgroundAudioManager.next()
					if (nextItem != null) {
						player.setMediaItem(buildMediaItem(nextItem))
						player.prepare()
						player.play()
					} else {
						BackgroundAudioManager.stop()
						stopSelf()
					}
				}
			}
		}

		override fun onPlayerError(error: PlaybackException) {
			BackgroundAudioManager.stop()
			stopSelf()
		}
	}

	private fun buildMediaItem(item: AudioQueueItem): MediaItem {
		return MediaItem.Builder()
			.setUri(Uri.parse(item.uri))
			.setMediaMetadata(
				MediaMetadata.Builder()
					.setTitle(item.displayName)
					.setArtist(item.artist.ifEmpty { "VideoMax" })
					.setAlbumTitle(item.album.ifEmpty { "" })
					.build()
			)
			.build()
	}

	private fun createInvisibleChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Audio en segundo plano",
				NotificationManager.IMPORTANCE_MIN
			).apply {
				description = "Reproducción de audio en segundo plano"
				lockscreenVisibility = Notification.VISIBILITY_SECRET
			}
			getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
		}
	}

	private fun buildNotification(): Notification {
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
			.setSmallIcon(android.R.drawable.ic_media_play)
			.setContentIntent(pendingIntent)
			.setPriority(Notification.PRIORITY_MIN)
			.setCategory(Notification.CATEGORY_SERVICE)
			.build()
	}

	private fun buildSessionActivity(): PendingIntent {
		val intent = Intent(this, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		return PendingIntent.getActivity(
			this, 0, intent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
	}

	override fun onDestroy() {
		BackgroundAudioManager.unregisterCallbacks()
		mediaSession?.run {
			player.removeListener(playbackListener)
			player.release()
			release()
		}
		mediaSession = null
		super.onDestroy()
	}

	companion object {
		const val EXTRA_VIDEO_URI = "video_uri"
		const val EXTRA_VIDEO_NAME = "video_name"
		const val EXTRA_VIDEO_ID = "video_id"
		const val EXTRA_AUTO_PLAY_NEXT = "auto_play_next"
		private const val CHANNEL_ID = "background_audio"
		private const val NOTIFICATION_ID = 1001
	}
}
