package com.puma.videomax.domain.model

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

enum class MusicSortOption {
	DATE_DESC,
	DATE_ASC,
	TITLE_ASC,
	TITLE_DESC,
	ARTIST_ASC,
	ARTIST_DESC,
	ALBUM_ASC,
	ALBUM_DESC,
	DURATION_DESC,
	DURATION_ASC,
	SIZE_DESC,
	SIZE_ASC,
	TRACK_NUMBER_ASC,
	TRACK_NUMBER_DESC
}

enum class ThemeMode {
	SYSTEM,
	LIGHT,
	DARK,
	AMOLED
}

data class AppSettings(
	val themeMode: ThemeMode = ThemeMode.SYSTEM,
	val sortOption: SortOption = SortOption.DATE_DESC,
	val musicSortOption: MusicSortOption = MusicSortOption.DATE_DESC,
	val accentColor: Long = 0L,
	val defaultPlaybackSpeed: Float = 1.0f,
	val rememberPlaybackPosition: Boolean = true,
	val autoPlayNext: Boolean = false,
	val seekStepSeconds: Int = 10,
	val showHiddenFiles: Boolean = false,
	val showNomedia: Boolean = false,
	val gesturesEnabled: Boolean = true,
	val autoPip: Boolean = false,
	val lastScanTimestamp: Long = 0L,
	val privateFolderPin: String? = null,
	val privateVideoIds: List<Long> = emptyList()
)

data class SubtitleTrack(
	val uri: String,
	val label: String,
	val mimeType: String,
	val language: String? = null
)
