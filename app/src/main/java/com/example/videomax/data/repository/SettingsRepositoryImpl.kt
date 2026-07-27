package com.example.videomax.data.repository

import com.example.videomax.data.local.datastore.SettingsDataStore
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.ThemeMode
import com.example.videomax.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
	private val dataStore: SettingsDataStore
) : SettingsRepository {

	override val settings: Flow<AppSettings> = dataStore.settings

	override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)

	override suspend fun setSortOption(option: SortOption) = dataStore.setSortOption(option)

	override suspend fun setDefaultPlaybackSpeed(speed: Float) =
		dataStore.setDefaultPlaybackSpeed(speed)

	override suspend fun setRememberPlaybackPosition(enabled: Boolean) =
		dataStore.setRememberPlaybackPosition(enabled)

	override suspend fun setAutoPlayNext(enabled: Boolean) = dataStore.setAutoPlayNext(enabled)

	override suspend fun setSeekStepSeconds(seconds: Int) = dataStore.setSeekStepSeconds(seconds)

	override suspend fun setShowHiddenFiles(enabled: Boolean) = dataStore.setShowHiddenFiles(enabled)

	override suspend fun setShowNomedia(enabled: Boolean) = dataStore.setShowNomedia(enabled)

	override suspend fun setGesturesEnabled(enabled: Boolean) = dataStore.setGesturesEnabled(enabled)

	override suspend fun setAutoPip(enabled: Boolean) = dataStore.setAutoPip(enabled)

	override suspend fun setLastScanTimestamp(timestamp: Long) = dataStore.setLastScanTimestamp(timestamp)

	override suspend fun setPrivateFolderPin(pin: String?) = dataStore.setPrivateFolderPin(pin)

	override suspend fun setPrivateVideoIds(ids: List<Long>) = dataStore.setPrivateVideoIds(ids)
}
