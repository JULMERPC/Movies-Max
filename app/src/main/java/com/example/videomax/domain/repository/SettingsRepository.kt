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
}
