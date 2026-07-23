package com.example.videomax.domain.repository

import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
	val settings: Flow<AppSettings>
	suspend fun setThemeMode(mode: ThemeMode)
	suspend fun setSortOption(option: SortOption)
	suspend fun setDefaultPlaybackSpeed(speed: Float)
	suspend fun setRememberPlaybackPosition(enabled: Boolean)
	suspend fun setAutoPlayNext(enabled: Boolean)
	suspend fun setSeekStepSeconds(seconds: Int)
	suspend fun setShowHiddenFiles(enabled: Boolean)
	suspend fun setGesturesEnabled(enabled: Boolean)
	suspend fun setAutoPip(enabled: Boolean)
	suspend fun setBlacklist(entries: List<String>)
	suspend fun addBlacklistEntry(entry: String)
	suspend fun removeBlacklistEntry(entry: String)
}
