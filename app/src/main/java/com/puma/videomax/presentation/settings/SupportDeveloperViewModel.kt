package com.puma.videomax.presentation.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puma.videomax.ads.AdsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RewardedAdState {
	LOADING,
	READY,
	SHOWING,
	COMPLETED,
	ERROR,
	UNAVAILABLE
}

@HiltViewModel
class SupportDeveloperViewModel @Inject constructor(
	private val adsManager: AdsManager
) : ViewModel() {

	private val _adState = MutableStateFlow(RewardedAdState.LOADING)
	val adState: StateFlow<RewardedAdState> = _adState.asStateFlow()

	private val _showThankYou = MutableStateFlow(false)
	val showThankYou: StateFlow<Boolean> = _showThankYou.asStateFlow()

	init {
		loadAd()
	}

	fun loadAd() {
		_adState.value = RewardedAdState.LOADING
		adsManager.loadRewarded(
			onLoaded = {
				_adState.value = RewardedAdState.READY
			}
		)
		if (_adState.value == RewardedAdState.LOADING) {
			viewModelScope.launch {
				kotlinx.coroutines.delay(8_000)
				if (_adState.value == RewardedAdState.LOADING) {
					_adState.value = RewardedAdState.UNAVAILABLE
				}
			}
		}
	}

	fun showAd(activity: Activity) {
		if (_adState.value != RewardedAdState.READY) return
		_adState.value = RewardedAdState.SHOWING
		adsManager.showRewarded(
			activity = activity,
			onUserEarned = {
				_adState.value = RewardedAdState.COMPLETED
				_showThankYou.value = true
			},
			onDismissed = {
				if (_adState.value != RewardedAdState.COMPLETED) {
					_adState.value = RewardedAdState.UNAVAILABLE
				}
				viewModelScope.launch {
					kotlinx.coroutines.delay(1_500)
					_showThankYou.value = false
					loadAd()
				}
			}
		)
	}

	fun dismissThankYou() {
		_showThankYou.value = false
	}
}
