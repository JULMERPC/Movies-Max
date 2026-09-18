package com.puma.videomax.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.puma.videomax.MainActivity
import com.puma.videomax.service.audio.AudioFocusHandler
import com.puma.videomax.service.audio.CodecFallbackPolicy

private const val TAG = "BackgroundAudioService"

/**
 * Background audio playback as a media foreground service.
 *
 * The notification (with Previous / Play-Pause / Next) is fully owned by
 * Media3's [DefaultMediaNotificationProvider]: no manual notifications,
 * no custom actions, no receivers — [MediaSessionService] promotes itself
 * to the foreground (type `mediaPlayback`, declared in the manifest) while
 * playing and updates the notification on every state change.
 *
 * Tapping the notification reopens [MainActivity] via the session activity.
 * The whole manager queue is loaded into ExoPlayer so previous/next are
 * real player commands and their buttons always work.
 */
class BackgroundAudioService : MediaSessionService() {

	private var mediaSession: MediaSession? = null
	private lateinit var notificationProvider: AudioNotificationProvider
	private lateinit var audioFocusHandler: AudioFocusHandler

	// ── Stop policy ─────────────────────────────────────────────
	// stopSelf() is ONLY allowed via finishPlaybackAndStop(): queue truly
	// exhausted, every item errored, or explicit user dismissal. Transient
	// ExoPlayer states (BUFFERING/IDLE during prepare, single bad file, slow
	// decoder init with MediaCodec warnings) must NEVER kill the service —
	// that was the eventType=19 → eventType=20 with no crash.
	private var consecutiveItemErrors = 0

	private val notificationManager: NotificationManager
		get() = getSystemService(NotificationManager::class.java)
	private val mainHandler = Handler(Looper.getMainLooper())
	private var positionUpdaterRunning = false
	private val positionUpdater = object : Runnable {
		override fun run() {
			// Runs ONLY while playing (started/stopped in onIsPlayingChanged).
			// No polling when paused: saves Handler wakeups + StateFlow emissions.
			val player = mediaSession?.player
			if (player != null && player.isPlaying) {
				BackgroundAudioManager.setPosition(player.currentPosition.coerceAtLeast(0L))
				mainHandler.postDelayed(this, POSITION_UPDATE_MS)
			} else {
				positionUpdaterRunning = false
			}
		}
	}

	private fun startPositionUpdater() {
		if (positionUpdaterRunning) return
		positionUpdaterRunning = true
		mainHandler.post(positionUpdater)
	}

	private fun stopPositionUpdater() {
		positionUpdaterRunning = false
		mainHandler.removeCallbacks(positionUpdater)
	}

	override fun onCreate() {
		super.onCreate()
		createPlaybackChannel()
		notificationProvider = AudioNotificationProvider(this)
		setMediaNotificationProvider(notificationProvider)
		audioFocusHandler = AudioFocusHandler(this).apply {
			onPause = {
				mediaSession?.player?.pause()
				BackgroundAudioManager.setPlaying(false)
			}
			// NOTE: no duck/volume writes here — Media3 owns focus + ducking
			// internally. A second writer flickered play/pause on every
			// transient focus event.
			onNoisy = {
				mediaSession?.player?.pause()
				BackgroundAudioManager.setPlaying(false)
			}
			attach()
		}
		// Prefer extension renderers (e.g. media3-decoder-ffmpeg for .ape/FLAC
		// complejos) over system MediaCodec; falls back to codec on failure.
		val renderersFactory = DefaultRenderersFactory(this)
			.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
			.setEnableDecoderFallback(true)
		val player = ExoPlayer.Builder(this, renderersFactory).build().apply {
			setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(C.USAGE_MEDIA)
					.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
					.build(),
				/* handleAudioFocus = */ true
			)
			// AUDIT(HIGH): PARTIAL_WAKE_LOCK while playing. Without it, Doze
			// sleeps the CPU with the screen off and background audio gaps out.
			setWakeMode(C.WAKE_MODE_LOCAL)
			setHandleAudioBecomingNoisy(true)
			addListener(playbackListener)
			playWhenReady = true
		}
		mediaSession = MediaSession.Builder(this, player)
			.setSessionActivity(buildSessionActivity())
			.setCallback(sessionCallback)
			.build()
		// Playback starts via startService (not via controller bind), so the
		// session must be registered explicitly. Without this, Media3 never
		// monitors the player and only our placeholder notification shows.
		addSession(mediaSession!!)

		BackgroundAudioManager.registerCallbacks(
			onTrackChange = { syncPlayerQueueAndPlay() },
			onPlayPauseChange = { playing ->
				// Single write path manager → player (guarded: no echo when
				// unchanged). Do NOT add a second collector on isPlaying.
				mediaSession?.player?.let { player ->
					if (player.playWhenReady != playing) player.playWhenReady = playing
				}
			},
			onSeekRequest = { positionMs ->
				mediaSession?.player?.seekTo(positionMs)
			}
		)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		// Media3 handles its own intents above (notification buttons, media
		// buttons, internal start-self): they always carry an action. If we
		// kept going, we'd reload the queue and restart playback on EVERY tap
		// (pause would instantly resume, next would jump back). Our own UI
		// intents have no action, only extras.
		if (intent?.action != null) return START_STICKY
		// Media3 only promotes to foreground once playback actually starts. If
		// buffering takes too long, Android 14 kills the service for missing
		// startForeground() in time — so promote immediately with a silent
		// placeholder that the media notification replaces below.
		ensureImmediateForeground()
		if (mediaSession == null) return START_NOT_STICKY

		val autoAdvance = intent?.getBooleanExtra(EXTRA_AUTO_PLAY_NEXT, false) ?: false

		val videoUri = intent?.getStringExtra(EXTRA_VIDEO_URI)
		val videoName = intent?.getStringExtra(EXTRA_VIDEO_NAME) ?: "Reproduciendo"

		val existingQueue = BackgroundAudioManager.queue.value
		val hasManagerQueue = existingQueue.isNotEmpty() && BackgroundAudioManager.current() != null

		if (hasManagerQueue && videoUri.isNullOrBlank()) {
			syncPlayerQueueAndPlay()
			return START_STICKY
		}

		if (videoUri.isNullOrBlank()) {
			// Idle/sticky-redelivery start (null intent after process death, or a
			// UI start with no extras): NEVER stopSelf here. If the player still
			// holds items, keep them; otherwise drop the placeholder foreground
			// and stay sticky until real work arrives.
			if ((mediaSession?.player?.mediaItemCount ?: 0) == 0 && !hasManagerQueue) {
				Log.i(TAG, "Idle start with empty queue: staying alive, no stopSelf")
				stopForeground(STOP_FOREGROUND_REMOVE)
			}
			return START_STICKY
		}

		val item = AudioQueueItem(
			videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0L),
			uri = videoUri,
			displayName = videoName,
			mimeType = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty()
		)
		BackgroundAudioManager.setQueue(
			items = listOf(item),
			startIndex = 0,
			autoAdvance = autoAdvance
		)
		syncPlayerQueueAndPlay()
		return START_STICKY
	}

	/**
	 * Loads the whole manager queue into ExoPlayer starting at the manager's
	 * current index. Previous/next become real available commands, so the
	 * notification buttons (and lockscreen/BT controls) always work.
	 *
	 * Idempotent: every song tap fires this TWICE (manager callback + service
	 * start intent). If the player already holds this exact queue+index and is
	 * not errored/ended, just resume instead of tearing down the timeline —
	 * each `setMediaItems()` restarts buffering and flickers play/pause.
	 */
	private var lastLoadedSignature: String? = null
	/** Cola cargada sin el índice: detecta recargas redundantes sobre el mismo contenido. */
	private var lastLoadedContentSignature: String? = null

	private fun syncPlayerQueueAndPlay() {
		val player = mediaSession?.player ?: return
		val queue = BackgroundAudioManager.queue.value
		if (queue.isEmpty()) return
		val startIndex = BackgroundAudioManager.currentIndex.value.coerceIn(0, queue.lastIndex)
		val signature = queueSignature(queue, startIndex)
		if (signature == lastLoadedSignature &&
			player.mediaItemCount == queue.size &&
			player.playbackState != Player.STATE_IDLE &&
			player.playerError == null
		) {
			if (BackgroundAudioManager.isPlaying.value && !player.isPlaying) player.play()
			return
		}
		// DOBLE-INICIO: si el contenido de la cola es el mismo y ExoPlayer ya
		// está parado sobre startIndex (p. ej. onTrackChange redundante tras una
		// autotransición AUTO), NO reconstruir el timeline: setMediaItems() +
		// prepare() reiniciarían la pista desde cero con micro-corte. Solo reanudar.
		if (queueContentSignature(queue) == lastLoadedContentSignature &&
			player.mediaItemCount == queue.size &&
			player.currentMediaItemIndex == startIndex &&
			player.playbackState != Player.STATE_IDLE &&
			player.playerError == null
		) {
			lastLoadedSignature = signature
			if (BackgroundAudioManager.isPlaying.value && !player.isPlaying) player.play()
			return
		}
		player.repeatMode =
			if (BackgroundAudioManager.repeatMode.value == RepeatMode.ONE) {
				Player.REPEAT_MODE_ONE
			} else {
				Player.REPEAT_MODE_OFF
			}
		player.setMediaItems(queue.map(::buildMediaItem), startIndex, 0L)
		lastLoadedSignature = signature
		lastLoadedContentSignature = queueContentSignature(queue)
		player.prepare()
		player.play()
		BackgroundAudioManager.setPosition(0L)
	}

	private fun queueSignature(queue: List<AudioQueueItem>, startIndex: Int): String {
		// AUDIT(BAJA): hash fold instead of concatenating every URI — full
		// libraries made this a multi-KB alloc on the main thread per tap.
		var h = startIndex * 31 + queue.size
		for (item in queue) {
			h = h * 31 + item.videoId.hashCode()
			h = h * 31 + item.uri.hashCode()
		}
		return h.toString()
	}

	private fun queueContentSignature(queue: List<AudioQueueItem>): String =
		queueSignature(queue, 0)

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
		return mediaSession
	}

	private val sessionCallback = object : MediaSession.Callback {
		override fun onConnect(
			session: MediaSession,
			controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
				.buildUpon()
				.add(SessionCommand(ACTION_TRANSPORT_TOGGLE, Bundle.EMPTY))
				.add(SessionCommand(ACTION_TRANSPORT_NEXT, Bundle.EMPTY))
				.add(SessionCommand(ACTION_TRANSPORT_PREV, Bundle.EMPTY))
				.build()
			return MediaSession.ConnectionResult.accept(
				sessionCommands,
				MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
			)
		}

		override fun onCustomCommand(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			command: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			val player = mediaSession?.player
			when (command.customAction) {
				ACTION_TRANSPORT_TOGGLE -> {
					// Key off playWhenReady, NOT isPlaying: during the
					// between-tracks buffering gap isPlaying=false while the
					// player is intentionally playing, and toggling on it
					// would swallow a real pause press (or vice versa).
					val willPlay = player?.playWhenReady != true
					player?.playWhenReady = willPlay
					BackgroundAudioManager.setPlaying(willPlay)
				}
				// Incremental O(1) navigation: ExoPlayer already holds the full
				// queue, so never rebuild it with setMediaItems() here.
				ACTION_TRANSPORT_NEXT -> advanceIncremental(+1)
				ACTION_TRANSPORT_PREV -> advanceIncremental(-1)
			}
			return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
		}
	}

	/**
	 * Moves one step inside the already-loaded ExoPlayer queue and mirrors the
	 * index in [BackgroundAudioManager] without re-emitting the queue.
	 * Falls back to the manager-driven reload only at the edges.
	 */
	private fun advanceIncremental(delta: Int) {
		val player = mediaSession?.player ?: return
		val target = BackgroundAudioManager.currentIndex.value + delta
		if (delta > 0 && player.hasNextMediaItem()) {
			player.seekToNextMediaItem()
			BackgroundAudioManager.seekTo(target)
			BackgroundAudioManager.setPosition(0L)
		} else if (delta < 0 && player.hasPreviousMediaItem()) {
			player.seekToPreviousMediaItem()
			BackgroundAudioManager.seekTo(target)
			BackgroundAudioManager.setPosition(0L)
		} else if (delta > 0) {
			BackgroundAudioManager.userNext()
		} else {
			BackgroundAudioManager.userPrevious()
		}
	}

	private val playbackListener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			val player = mediaSession?.player
			if (isPlaying) {
				BackgroundAudioManager.setPlaying(true)
				startPositionUpdater()
			} else {
				stopPositionUpdater()
				// The gap between tracks is BUFFERING with playWhenReady=true.
				// Mirroring it as paused would clear playWhenReady and the next
				// track would start paused — the notification-next stall.
				// Only a settled pause (READY/ENDED/IDLE) counts.
				if (player == null || player.playbackState != Player.STATE_BUFFERING) {
					BackgroundAudioManager.setPlaying(false)
				}
			}
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			val player = mediaSession?.player ?: return
			when (playbackState) {
				Player.STATE_READY -> {
					// An item became playable → previous item errors are forgiven.
					consecutiveItemErrors = 0
					BackgroundAudioManager.setDuration(player.duration.coerceAtLeast(0L))
					// Pause pressed mid-buffering never fires onIsPlayingChanged
					// (it was already false): settle the manager state here.
					if (!player.playWhenReady) BackgroundAudioManager.setPlaying(false)
				}
				Player.STATE_ENDED -> {
					// ENDED fires only at true playlist end (auto-advance emits
					// onMediaItemTransition instead). Stop ONLY if the manager
					// queue is also exhausted.
					consecutiveItemErrors = 0
					BackgroundAudioManager.setPosition(0L)
					val nextItem = BackgroundAudioManager.next()
					if (nextItem != null) {
						syncPlayerQueueAndPlay()
					} else {
						finishPlaybackAndStop("queue exhausted at natural end")
					}
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			// Mirror the player index into the manager. AUTO covers natural
			// advance; SEEK covers BT/lockscreen/Auto/next-external presses,
			// which bypass our custom commands — without this the manager
			// index (queue highlight, next-target) drifts from the player.
			// PLAYLIST_CHANGED (our own reloads) is ignored; seekTo(Int) is
			// idempotent so double-mirroring is harmless.
			//
			// DOBLE-INICIO: en transiciones AUTO ExoPlayer ya preparó el
			// siguiente item con buffering continuo. NUNCA llamar aquí a
			// seekTo(0), prepare() ni play(): la doble llamada reinicia la
			// pista desde cero con un micro-corte audible. Solo se espeja el
			// índice y se resetea el progreso de UI.
			if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
				reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
			) {
				consecutiveItemErrors = 0
				mediaSession?.player?.let { player ->
					BackgroundAudioManager.seekTo(player.currentMediaItemIndex)
				}
				BackgroundAudioManager.setPosition(0L)
			}
		}

		override fun onPlayerError(error: PlaybackException) {
			// Namida-style tolerance: skip the bad item, keep the service alive.
			// The service dies ONLY when every queued item has failed in a row
			// (unplayable queue) — never on the first decoder/IO hiccup during
			// init, which was the premature eventType=20.
			val current = BackgroundAudioManager.current()
			val queueSize = BackgroundAudioManager.queue.value.size.coerceAtLeast(1)
			consecutiveItemErrors++
			Log.w(
				TAG,
				"Item error $consecutiveItemErrors/$queueSize " +
					"uri=${current?.uri} mime=${current?.mimeType} code=${error.errorCodeName}",
				error
			)
			if (consecutiveItemErrors >= queueSize) {
				finishPlaybackAndStop("all $queueSize queued items failed")
				return
			}
			val player = mediaSession?.player
			if (player != null && player.hasNextMediaItem()) {
				player.seekToNextMediaItem()
				BackgroundAudioManager.seekTo(BackgroundAudioManager.currentIndex.value + 1)
			} else {
				val nextItem = BackgroundAudioManager.next()
				if (nextItem != null) {
					syncPlayerQueueAndPlay()
				} else {
					finishPlaybackAndStop("no next item after error")
					return
				}
			}
			BackgroundAudioManager.setPosition(0L)
		}
	}

	/**
	 * THE single place allowed to kill the service: natural queue end, fully
	 * unplayable queue, or explicit user dismissal ([onTaskRemoved] while idle).
	 */
	private fun finishPlaybackAndStop(reason: String) {
		Log.i(TAG, "Stopping service: $reason")
		consecutiveItemErrors = 0
		BackgroundAudioManager.stop()
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		// Recents swipe-away: keep playing like any music app; stop only if the
		// player is already idle (explicit user dismissal, no playback to keep).
		if (mediaSession?.player?.isPlaying != true) {
			finishPlaybackAndStop("task removed while idle")
		}
		super.onTaskRemoved(rootIntent)
	}

	private fun buildMediaItem(item: AudioQueueItem): MediaItem {
		val builder = MediaItem.Builder()
			.setUri(Uri.parse(item.uri))
			.setMediaMetadata(
				MediaMetadata.Builder()
					.setTitle(item.displayName)
					.setArtist(item.artist.ifEmpty { "Jualix" })
					.setAlbumTitle(item.album.ifEmpty { "" })
					.build()
			)
		if (item.mimeType.isNotBlank()) {
			builder.setMimeType(
				when {
					CodecFallbackPolicy.isUnsupportedApe(item.mimeType) -> MimeTypes.AUDIO_UNKNOWN
					else -> item.mimeType
				}
			)
		}
		return builder.build()
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

	// ── Immediate foreground ──────────────────────────────────────

	private fun createPlaybackChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Reproducción",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Reproducción de audio en segundo plano"
				setShowBadge(false)
			}
			notificationManager.createNotificationChannel(channel)
		}
	}

	private fun ensureImmediateForeground() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) {
			Log.w(TAG, "POST_NOTIFICATIONS not granted: media notification may be hidden")
		}
		startForegroundSafe(buildPlaceholderNotification(), NOTIFICATION_ID)
	}

	private fun buildPlaceholderNotification(): Notification {
		// Real track metadata from the start: if the player stalls (e.g. EOS
		// on a bad stream) before Media3 publishes, the user still sees what
		// was supposed to play instead of a generic "preparing" text.
		val current = BackgroundAudioManager.current()
		return NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_media_play)
			.setContentTitle(current?.displayName ?: "Jualix")
			.setContentText(current?.artist?.ifEmpty { "Jualix" } ?: "Jualix")
			.setContentIntent(buildSessionActivity())
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setSilent(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.build()
	}

	private fun startForegroundSafe(notification: Notification, id: Int) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				startForeground(
					id,
					notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
				)
			} else {
				@Suppress("DEPRECATION")
				startForeground(id, notification)
			}
		} catch (e: ForegroundServiceStartNotAllowedException) {
			Log.e(TAG, "startForeground not allowed (background start restriction)", e)
		} catch (e: SecurityException) {
			Log.e(TAG, "startForeground SecurityException (missing permission/type?)", e)
		} catch (e: Exception) {
			Log.e(TAG, "startForeground unexpected failure", e)
		}
	}

	override fun onDestroy() {
		stopPositionUpdater()
		runCatching { audioFocusHandler.detach() }
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
		const val EXTRA_MIME_TYPE = "mime_type"

		private const val POSITION_UPDATE_MS = 500L
		private const val CHANNEL_ID = "media_playback"
		private const val NOTIFICATION_ID = 1001
	}
}
