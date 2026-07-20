package com.example.videomax.domain.model

data class PlaybackHistory(
	val videoId: Long,
	val videoUri: String,
	val displayName: String,
	val positionMs: Long,
	val durationMs: Long,
	val watchedAt: Long,
	val thumbnailUri: String = videoUri
) {
	val progressFraction: Float
		get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
