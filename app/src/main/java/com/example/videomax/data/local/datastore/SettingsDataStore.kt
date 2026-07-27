package com.example.videomax.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
	name = "videomax_settings"
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
		val showHiddenFiles = booleanPreferencesKey("show_hidden_files")
		val showNomedia = booleanPreferencesKey("show_nomedia")
		val gesturesEnabled = booleanPreferencesKey("gestures_enabled")
		val autoPip = booleanPreferencesKey("auto_pip")
		val lastScanTimestamp = longPreferencesKey("last_scan_timestamp")
		val privateFolderPin = stringPreferencesKey("private_folder_pin")
		val privateVideoIds = stringPreferencesKey("private_video_ids")
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
			seekStepSeconds = prefs[Keys.seekStep] ?: 10,
			showHiddenFiles = prefs[Keys.showHiddenFiles] ?: false,
			showNomedia = prefs[Keys.showNomedia] ?: false,
			gesturesEnabled = prefs[Keys.gesturesEnabled] ?: true,
			autoPip = prefs[Keys.autoPip] ?: false,
			lastScanTimestamp = prefs[Keys.lastScanTimestamp] ?: 0L,
			privateFolderPin = prefs[Keys.privateFolderPin],
			privateVideoIds = decodeLongList(prefs[Keys.privateVideoIds])
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

	suspend fun setShowHiddenFiles(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.showHiddenFiles] = enabled }
	}

	suspend fun setGesturesEnabled(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.gesturesEnabled] = enabled }
	}

	suspend fun setAutoPip(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.autoPip] = enabled }
	}

	suspend fun setLastScanTimestamp(timestamp: Long) {
		context.settingsDataStore.edit {
			it[Keys.lastScanTimestamp] = timestamp
		}
	}

	suspend fun setShowNomedia(enabled: Boolean) {
		context.settingsDataStore.edit { it[Keys.showNomedia] = enabled }
	}

	suspend fun setPrivateFolderPin(pin: String?) {
		context.settingsDataStore.edit {
			if (pin == null) it.remove(Keys.privateFolderPin)
			else it[Keys.privateFolderPin] = pin
		}
	}

	suspend fun setPrivateVideoIds(ids: List<Long>) {
		context.settingsDataStore.edit {
			it[Keys.privateVideoIds] = encodeLongList(ids)
		}
	}

	private fun encodeLongList(ids: List<Long>): String =
		org.json.JSONArray(ids.map { it }).toString()

	private fun decodeLongList(raw: String?): List<Long> {
		if (raw.isNullOrBlank()) return emptyList()
		return runCatching {
			org.json.JSONArray(raw).let { arr ->
				(0 until arr.length()).map { arr.getLong(it) }
			}
		}.getOrDefault(emptyList())
	}
}
