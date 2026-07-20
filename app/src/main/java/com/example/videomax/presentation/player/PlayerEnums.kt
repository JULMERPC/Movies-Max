package com.example.videomax.presentation.player

/**
 * Player orientation preference. AUTO follows the device sensor.
 */
enum class PlayerOrientation {
	AUTO,
	PORTRAIT,
	LANDSCAPE
}

/**
 * Queue repeat behaviour when a video ends or Next is pressed on the last item.
 */
enum class QueueRepeatMode {
	OFF,
	ONE,
	ALL
}
