package com.example.videomax.domain.model

/**
 * Domain model representing a video discovered on the device.
 */
data class Video(
	val id: Long,
	val uri: String,
	val displayName: String,
	val path: String?,
	val durationMs: Long,
	val sizeBytes: Long,
	val width: Int,
	val height: Int,
	val mimeType: String,
	val dateAdded: Long,
	val dateModified: Long,
	val folderName: String,
	val isFavorite: Boolean = false,
	val lastPositionMs: Long = 0L,
	val codec: String? = null,
	val playCount: Int = 0
) {
	val resolutionLabel: String
		get() = if (width > 0 && height > 0) "${width}x${height}" else "—"

	val isVertical: Boolean
		get() = height > width
}
