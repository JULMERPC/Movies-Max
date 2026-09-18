package com.puma.videomax.service

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList

const val ACTION_TRANSPORT_TOGGLE = "com.puma.videomax.action.TRANSPORT_TOGGLE"
const val ACTION_TRANSPORT_NEXT = "com.puma.videomax.action.TRANSPORT_NEXT"
const val ACTION_TRANSPORT_PREV = "com.puma.videomax.action.TRANSPORT_PREV"

/**
 * Media3 notification with app-owned transport buttons.
 *
 * Instead of the default player-command buttons, the notification carries
 * three custom commands (prev / toggle / next) that are dispatched to
 * [MediaSession.Callback.onCustomCommand], where they drive ExoPlayer and
 * [BackgroundAudioManager] directly. Foreground promotion, channel and
 * refresh stay 100% owned by Media3.
 */
class AudioNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {

	override fun getMediaButtons(
		session: MediaSession,
		playerCommands: Player.Commands,
		mediaButtonPreferences: ImmutableList<CommandButton>,
		showPauseButton: Boolean
	): ImmutableList<CommandButton> {
		return ImmutableList.of(
			CommandButton.Builder(CommandButton.ICON_PREVIOUS)
				.setSessionCommand(SessionCommand(ACTION_TRANSPORT_PREV, Bundle.EMPTY))
				.setSlots(CommandButton.SLOT_BACK)
				.setDisplayName("Anterior")
				.build(),
			CommandButton.Builder(
				if (showPauseButton) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY
			)
				.setSessionCommand(SessionCommand(ACTION_TRANSPORT_TOGGLE, Bundle.EMPTY))
				.setSlots(CommandButton.SLOT_CENTRAL)
				.setDisplayName(if (showPauseButton) "Pausar" else "Reproducir")
				.build(),
			CommandButton.Builder(CommandButton.ICON_NEXT)
				.setSessionCommand(SessionCommand(ACTION_TRANSPORT_NEXT, Bundle.EMPTY))
				.setSlots(CommandButton.SLOT_FORWARD)
				.setDisplayName("Siguiente")
				.build()
		)
	}
}
