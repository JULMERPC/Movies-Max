package com.example.videomax.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
	name = "video_player_pro_settings"
)

@Singleton
class SettingsDataStore @Inject constructor(
	@param:ApplicationContext private val context: Context
) {
	private object Keys {
		val themeMode = stringPreferencesKey("theme_mode")
		val sortOption = stringPreferencesKey("sort_option")
		val playbackSpeed = floatPreferencesKey("playback_speed")
		val rememberPosition = booleanPreferencesKey("remember_position")
		val autoPlayNext = booleanPreferencesKey("auto_play_next")
		val seekStep = intPreferencesKey("seek_step_seconds")
	}

	val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
		AppSettings(
			themeMode = prefs[Keys.themeMode]?.let {
				runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
			} ?: ThemeMode.SYSTEM,
			sortOption = prefs[Keys.sortOption]?.let {
				runCatching { SortOption.valueOf(it) }.getOrDefault(SortOption.DATE_DESC)
			} ?: SortOption.DATE_DESC,
			defaultPlaybackSpeed = prefs[Keys.playbackSpeed] ?: 1.0f,
			rememberPlaybackPosition = prefs[Keys.rememberPosition] ?: true,
			autoPlayNext = prefs[Keys.autoPlayNext] ?: false,
			seekStepSeconds = prefs[Keys.seekStep] ?: 10
		)
	}

	suspend fun setThemeMode(mode: ThemeMode) {
		context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
	}

	suspend fun setSortOption(option: SortOption) {
		context.settingsDataStore.edit { it[Keys.sortOption] = option.name }
	}

	suspend fun setDefaultPlaybackSpeed(speed: Float) {
		context.settingsDataStore.edit { it[Keys.playbackSpeed] = speed }
	}

	suspend fun setRememberPlaybackPosition(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.rememberPosition] = enabled }
	}

	suspend fun setAutoPlayNext(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.autoPlayNext] = enabled }
	}

	suspend fun setSeekStepSeconds(seconds: Int) {
		context.settingsDataStore.edit { it[Keys.seekStep] = seconds }
	}
}
