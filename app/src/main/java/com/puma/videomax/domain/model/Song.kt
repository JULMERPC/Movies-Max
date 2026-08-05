package com.puma.videomax.domain.model

data class Song(
	val id: Long,
	val uri: String,
	val title: String,
	val artist: String,
	val album: String,
	val albumId: Long,
	val durationMs: Long,
	val sizeBytes: Long,
	val mimeType: String,
	val dateAdded: Long,
	val dateModified: Long,
	val path: String?,
	val folderName: String,
	val trackNumber: Int,
	val discNumber: Int,
	val year: Int,
	val bitrate: Int,
	val sampleRate: Int,
	val isFavorite: Boolean = false,
	val lastPositionMs: Long = 0L,
	val playCount: Int = 0,
	val trackGain: Float? = null,
	val albumGain: Float? = null,
	val trackPeak: Float? = null,
	val albumPeak: Float? = null
) {
	val durationLabel: String
		get() {
			val totalSec = durationMs / 1000
			val min = totalSec / 60
			val sec = totalSec % 60
			return "$min:${sec.toString().padStart(2, '0')}"
		}

	val bitrateLabel: String
		get() = if (bitrate > 0) "${bitrate / 1000} kbps" else "—"

	val formatLabel: String
		get() = when {
			mimeType.contains("flac", true) -> "FLAC"
			mimeType.contains("aac", true) -> "AAC"
			mimeType.contains("opus", true) -> "Opus"
			mimeType.contains("ogg", true) -> "OGG"
			mimeType.contains("wav", true) -> "WAV"
			mimeType.contains("m4a", true) -> "M4A"
			mimeType.contains("amr", true) -> "AMR"
			else -> "MP3"
		}
}
