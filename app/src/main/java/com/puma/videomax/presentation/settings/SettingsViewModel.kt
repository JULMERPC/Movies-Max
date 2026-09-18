package com.puma.videomax.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puma.videomax.billing.PremiumDataSource
import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.model.ThemeMode
import com.puma.videomax.domain.monetization.MonetizationRepository
import com.puma.videomax.domain.monetization.MonetizationState
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
	observeSettings: ObserveSettingsUseCase,
	private val settingsRepository: SettingsRepository,
	private val premiumDataSource: PremiumDataSource,
	monetization: MonetizationRepository
) : ViewModel() {

	val settings: StateFlow<AppSettings> = observeSettings()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

	val monetizationState: StateFlow<MonetizationState> = monetization.state

	private val _removeAdsPrice = MutableStateFlow<String?>(null)
	val removeAdsPrice: StateFlow<String?> = _removeAdsPrice.asStateFlow()

	private val _purchaseMessage = MutableStateFlow<String?>(null)
	val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

	init {
		viewModelScope.launch {
			_removeAdsPrice.value = runCatching { premiumDataSource.getRemoveAdsPrice() }.getOrNull()
		}
	}

	fun buyRemoveAds(activity: android.app.Activity) {
		viewModelScope.launch {
			val ok = runCatching { premiumDataSource.launchRemoveAdsPurchase(activity) }
				.getOrDefault(false)
			if (ok) return@launch
			// El flujo no se abrió (p. ej. ITEM_ALREADY_OWNED): antes de mostrar
			// un error, se intenta restaurar la compra existente en Play.
			announceRestoreResult()
		}
	}

	fun restorePurchases() {
		viewModelScope.launch {
			_purchaseMessage.value = "Buscando tu compra en Google Play..."
			announceRestoreResult()
		}
	}

	private suspend fun announceRestoreResult() {
		val restored = runCatching { premiumDataSource.restorePurchases() }
			.getOrDefault(false)
		if (restored) {
			_purchaseMessage.value = "Compra restaurada: Jualix Premium ya está activo."
			return
		}
		val pending = runCatching { premiumDataSource.hasPendingRemoveAdsPurchase() }
			.getOrDefault(false)
		_purchaseMessage.value = if (pending) {
			"Tu pago figura como pendiente en Google Play. Completalo en la app de Play Store y volvé a restaurar."
		} else {
			"No se encontró ninguna compra en esta cuenta de Play. Si pagaste con otra cuenta, cambiala en Play Store y restaurá de nuevo."
		}
	}

	fun clearPurchaseMessage() {
		_purchaseMessage.value = null
	}

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
}
