package com.puma.videomax.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.puma.videomax.domain.monetization.MonetizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared per-screen gate for native list slots: visible only with UMP
 * consent AND without Premium/24h pass. Collecting the repository keeps
 * every slot in sync (e.g. a pass earned mid-scroll hides all slots).
 */
@HiltViewModel
class NativeAdViewModel @Inject constructor(
    consentManager: ConsentManager,
    monetization: MonetizationRepository,
    private val loader: NativeAdLoader
) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    init {
        viewModelScope.launch {
            consentManager.canRequestAds.collect { consent ->
                _visible.value = consent && !monetization.state.value.areAdsRemoved()
            }
        }
        viewModelScope.launch {
            monetization.state.collect { state ->
                _visible.value = consentManager.canRequestAds.value && !state.areAdsRemoved()
            }
        }
    }

    suspend fun loadAd(): NativeAd? = loader.load()
}
