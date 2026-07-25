package com.example.videomax.domain.model

enum class SortOption {
	DATE_DESC,
	DATE_ASC,
	NAME_ASC,
	NAME_DESC,
	DURATION_DESC,
	DURATION_ASC,
	SIZE_DESC,
	SIZE_ASC
}

enum class VideoFilter {
	ALL,
	FAVORITES,
	RECENT,
	FOLDERS
}

enum class ThemeMode {
	SYSTEM,
	LIGHT,
	DARK
}

data class AppSettings(
	val themeMode: ThemeMode = ThemeMode.SYSTEM,
	val sortOption: SortOption = SortOption.DATE_DESC,
	val defaultPlaybackSpeed: Float = 1.0f,
	val rememberPlaybackPosition: Boolean = true,
	val autoPlayNext: Boolean = false,
	val seekStepSeconds: Int = 10,
	val showHiddenFiles: Boolean = false,
	val gesturesEnabled: Boolean = true,
	val autoPip: Boolean = false,
	val blacklist: List<String> = emptyList(),
	val lastScanTimestamp: Long = 0L
)

data class SubtitleTrack(
	val uri: String,
	val label: String,
	val mimeType: String,
	val language: String? = null
)
