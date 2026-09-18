package com.puma.videomax.presentation.settings

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puma.videomax.ads.AdsManager
import com.puma.videomax.domain.monetization.MonetizationRepository
import com.puma.videomax.domain.monetization.MonetizationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SupportDeveloperVM"
private const val TICK_MS = 30_000L

enum class RewardedAdState {
	LOADING,
	READY,
	SHOWING,
	COMPLETED,
	ERROR,
	UNAVAILABLE
}

/**
 * "Apoya al desarrollador": 2 rewarded ads = 24h ad-free pass.
 *
 * - [monetizationState] mirrors [MonetizationRepository.state]
 *   (progress 0..2, pass expiry, Premium).
 * - [passRemainingMs] ticks every 30s while the pass is active so the UI
 *   can show a live countdown without polling DataStore.
 * - Rewarded ads are never loaded while ads are already removed
 *   (Premium/pass active) to avoid wasting impressions.
 */
@HiltViewModel
class SupportDeveloperViewModel @Inject constructor(
	private val adsManager: AdsManager,
	private val monetization: MonetizationRepository
) : ViewModel() {

	private val _adState = MutableStateFlow(RewardedAdState.LOADING)
	val adState: StateFlow<RewardedAdState> = _adState.asStateFlow()

	private val _showThankYou = MutableStateFlow(false)
	val showThankYou: StateFlow<Boolean> = _showThankYou.asStateFlow()

	val monetizationState: StateFlow<MonetizationState> = monetization.state

	private val _passRemainingMs = MutableStateFlow(0L)
	val passRemainingMs: StateFlow<Long> = _passRemainingMs.asStateFlow()

	init {
		viewModelScope.launch {
			monetization.state.collect { state ->
				_passRemainingMs.value = remainingMs(state)
			}
		}
		viewModelScope.launch {
			while (true) {
				delay(TICK_MS)
				_passRemainingMs.value = remainingMs(monetization.state.value)
			}
		}
		loadAd()
	}

	fun loadAd() {
		if (monetization.state.value.areAdsRemoved()) {
			Log.d(TAG, "loadAd skipped: ads already removed (Premium/pass)")
			return
		}
		_adState.value = RewardedAdState.LOADING
		adsManager.loadRewarded(
			onLoaded = {
				_adState.value = RewardedAdState.READY
			}
		)
		if (_adState.value == RewardedAdState.LOADING) {
			viewModelScope.launch {
				delay(8_000)
				if (_adState.value == RewardedAdState.LOADING) {
					_adState.value = RewardedAdState.UNAVAILABLE
				}
			}
		}
	}

	fun showAd(activity: Activity) {
		if (_adState.value != RewardedAdState.READY) return
		if (monetization.state.value.areAdsRemoved()) {
			Log.d(TAG, "showAd skipped: ads already removed (Premium/pass)")
			return
		}
		_adState.value = RewardedAdState.SHOWING
		adsManager.showRewarded(
			activity = activity,
			onUserEarned = {
				viewModelScope.launch {
					val passEarned = monetization.registerRewardedCompleted()
					Log.d(TAG, "Rewarded completed, passEarned=$passEarned")
				}
				_adState.value = RewardedAdState.COMPLETED
				_showThankYou.value = true
			},
			onDismissed = {
				if (_adState.value != RewardedAdState.COMPLETED) {
					_adState.value = RewardedAdState.UNAVAILABLE
				}
				viewModelScope.launch {
					delay(1_500)
					_showThankYou.value = false
					loadAd()
				}
			}
		)
	}

	fun dismissThankYou() {
		_showThankYou.value = false
	}

	private fun remainingMs(state: MonetizationState): Long =
		(state.adFreeUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
}

/**
 * Formats a pass countdown for display: "23h 45m", "45m", "2h", "menos de 1 min".
 * Pure function — covered by unit tests.
 */
fun formatPassRemaining(remainingMs: Long): String {
	if (remainingMs <= 0L) return "menos de 1 min"
	val totalMinutes = remainingMs / 60_000L
	val hours = totalMinutes / 60
	val minutes = totalMinutes % 60
	return when {
		hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
		hours > 0 -> "${hours}h"
		minutes > 0 -> "${minutes}m"
		else -> "menos de 1 min"
	}
}
