package com.puma.videomax.domain.repository

import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.SortOption
import com.puma.videomax.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
	val settings: Flow<AppSettings>
	suspend fun setThemeMode(mode: ThemeMode)
	suspend fun setSortOption(option: SortOption)
	suspend fun setMusicSortOption(option: MusicSortOption)
	suspend fun setAccentColor(color: Long)
	suspend fun setDefaultPlaybackSpeed(speed: Float)
	suspend fun setRememberPlaybackPosition(enabled: Boolean)
	suspend fun setAutoPlayNext(enabled: Boolean)
	suspend fun setSeekStepSeconds(seconds: Int)
	suspend fun setShowHiddenFiles(enabled: Boolean)
	suspend fun setShowNomedia(enabled: Boolean)
	suspend fun setGesturesEnabled(enabled: Boolean)
	suspend fun setAutoPip(enabled: Boolean)
	suspend fun setLastScanTimestamp(timestamp: Long)
	suspend fun setPrivateFolderPin(pin: String?)
	suspend fun setPrivateVideoIds(ids: List<Long>)
}
