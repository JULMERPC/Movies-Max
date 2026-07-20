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
import com.example.videomax.domain.model.AppLanguage
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
		val language = stringPreferencesKey("language")
		val showHiddenFiles = booleanPreferencesKey("show_hidden_files")
		val gesturesEnabled = booleanPreferencesKey("gestures_enabled")
		val autoPip = booleanPreferencesKey("auto_pip")
		val blacklist = stringPreferencesKey("blacklist")
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
			language = prefs[Keys.language]?.let {
				runCatching { AppLanguage.valueOf(it) }.getOrDefault(AppLanguage.SYSTEM)
			} ?: AppLanguage.SYSTEM,
			showHiddenFiles = prefs[Keys.showHiddenFiles] ?: false,
			gesturesEnabled = prefs[Keys.gesturesEnabled] ?: true,
			autoPip = prefs[Keys.autoPip] ?: false,
			blacklist = decodeBlacklist(prefs[Keys.blacklist])
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

	suspend fun setLanguage(language: AppLanguage) {
		context.settingsDataStore.edit { it[Keys.language] = language.name }
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

	suspend fun setBlacklist(entries: List<String>) {
		context.settingsDataStore.edit {
			it[Keys.blacklist] = encodeBlacklist(entries)
		}
	}

	suspend fun addBlacklistEntry(entry: String) {
		val clean = entry.trim()
		if (clean.isEmpty()) return
		context.settingsDataStore.edit { prefs ->
			val current = decodeBlacklist(prefs[Keys.blacklist]).toMutableList()
			if (current.none { it.equals(clean, ignoreCase = true) }) {
				current += clean
			}
			prefs[Keys.blacklist] = encodeBlacklist(current)
		}
	}

	suspend fun removeBlacklistEntry(entry: String) {
		context.settingsDataStore.edit { prefs ->
			val current = decodeBlacklist(prefs[Keys.blacklist])
				.filterNot { it.equals(entry, ignoreCase = true) }
			prefs[Keys.blacklist] = encodeBlacklist(current)
		}
	}

	private fun encodeBlacklist(entries: List<String>): String =
		entries.joinToString("\u001F") { it.trim() }.trim()

	private fun decodeBlacklist(raw: String?): List<String> =
		raw?.split('\u001F')
			?.map { it.trim() }
			?.filter { it.isNotEmpty() }
			.orEmpty()
}
