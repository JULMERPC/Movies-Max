package com.puma.videomax.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.model.ThemeMode
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
	observeSettings: ObserveSettingsUseCase,
	private val settingsRepository: SettingsRepository
) : ViewModel() {

	val settings: StateFlow<AppSettings> = observeSettings()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

	fun setTheme(mode: ThemeMode) {
		viewModelScope.launch { settingsRepository.setThemeMode(mode) }
	}

	fun setSpeed(speed: Float) {
		viewModelScope.launch { settingsRepository.setDefaultPlaybackSpeed(speed) }
	}

	fun setRememberPosition(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setRememberPlaybackPosition(enabled) }
	}

	fun setAutoPlayNext(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setAutoPlayNext(enabled) }
	}

	fun setSeekStep(seconds: Int) {
		viewModelScope.launch { settingsRepository.setSeekStepSeconds(seconds) }
	}

	fun setShowHiddenFiles(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setShowHiddenFiles(enabled) }
	}

	fun setShowNomedia(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setShowNomedia(enabled) }
	}

	fun setGesturesEnabled(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setGesturesEnabled(enabled) }
	}

	fun setAutoPip(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setAutoPip(enabled) }
	}

	fun setPrivateFolderPin(pin: String?) {
		viewModelScope.launch { settingsRepository.setPrivateFolderPin(pin) }
	}

	fun setPrivateVideoIds(ids: List<Long>) {
		viewModelScope.launch { settingsRepository.setPrivateVideoIds(ids) }
	}

	fun setAccentColor(color: Long) {
		viewModelScope.launch { settingsRepository.setAccentColor(color) }
	}
}
