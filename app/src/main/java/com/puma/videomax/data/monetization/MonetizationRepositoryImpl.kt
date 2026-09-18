package com.puma.videomax.data.monetization

import android.util.Log
import com.puma.videomax.billing.PremiumDataSource
import com.puma.videomax.data.local.datastore.SettingsDataStore
import com.puma.videomax.domain.monetization.MonetizationRepository
import com.puma.videomax.domain.monetization.MonetizationRepository.Companion.AD_FREE_PASS_MS
import com.puma.videomax.domain.monetization.MonetizationRepository.Companion.REWARDED_REQUIRED
import com.puma.videomax.domain.monetization.MonetizationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MonetizationRepository"

/**
 * DataStore-backed implementation. Loads persisted state on creation and
 * keeps an in-memory [StateFlow] as the single source of truth for the UI
 * and [com.puma.videomax.ads.AdController].
 */
@Singleton
class MonetizationRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val premiumDataSource: PremiumDataSource
) : MonetizationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(MonetizationState())
    override val state: StateFlow<MonetizationState> = _state.asStateFlow()

    init {
        scope.launch {
            val premium = runCatching { premiumDataSource.isRemoveAdsPurchased() }
                .getOrDefault(false)
            if (premium) {
                settingsDataStore.setPremiumRemoveAds(true)
            }
            _state.value = MonetizationState(
                isPremium = settingsDataStore.isPremiumRemoveAds(),
                adFreeUntilMs = settingsDataStore.getAdFreeUntil(),
                rewardedProgress = settingsDataStore.getRewardedProgress()
                    .coerceIn(0, REWARDED_REQUIRED),
                lastInterstitialShownAtMs = settingsDataStore.getLastInterstitialShownAt()
            )
            Log.d(TAG, "Loaded monetization state: ${_state.value}")
        }
        // One-way sync: purchases (Play Billing or fake) flow here and persist.
        scope.launch {
            premiumDataSource.premiumUpdates.collect { purchased ->
                if (purchased && !_state.value.isPremium) {
                    setPremium(true)
                }
            }
        }
    }

    override suspend fun registerRewardedCompleted(): Boolean {
        val current = _state.value
        val next = (current.rewardedProgress + 1).coerceAtMost(REWARDED_REQUIRED)
        return if (next >= REWARDED_REQUIRED) {
            val until = System.currentTimeMillis() + AD_FREE_PASS_MS
            settingsDataStore.setAdFreeUntil(until)
            settingsDataStore.setRewardedProgress(0)
            _state.value = current.copy(adFreeUntilMs = until, rewardedProgress = 0)
            Log.d(TAG, "Ad-free pass earned until=$until")
            true
        } else {
            settingsDataStore.setRewardedProgress(next)
            _state.value = current.copy(rewardedProgress = next)
            Log.d(TAG, "Rewarded progress: $next/$REWARDED_REQUIRED")
            false
        }
    }

    override suspend fun setPremium(purchased: Boolean) {
        settingsDataStore.setPremiumRemoveAds(purchased)
        _state.value = _state.value.copy(isPremium = purchased)
        Log.d(TAG, "Premium set to $purchased")
    }

    override suspend fun markInterstitialShown() {
        val now = System.currentTimeMillis()
        settingsDataStore.setLastInterstitialShownAt(now)
        _state.value = _state.value.copy(lastInterstitialShownAtMs = now)
    }

    override suspend fun clearAdFreePass() {
        settingsDataStore.setAdFreeUntil(0L)
        settingsDataStore.setRewardedProgress(0)
        _state.value = _state.value.copy(adFreeUntilMs = 0L, rewardedProgress = 0)
        Log.d(TAG, "Ad-free pass cleared (debug)")
    }
}
