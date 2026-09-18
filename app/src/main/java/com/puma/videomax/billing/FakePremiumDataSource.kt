package com.puma.videomax.billing

import android.app.Activity
import android.util.Log
import com.puma.videomax.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FakePremiumDataSource"

/**
 * Local stand-in: persists the "Remove Ads" flag so the full Premium UX
 * (Settings entry, ad gating) works end-to-end without Play Console setup.
 * Replaced by [PlayBillingPremiumDataSource] in production builds.
 */
@Singleton
class FakePremiumDataSource @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : PremiumDataSource {

    private val _premiumUpdates = MutableStateFlow(false)
    override val premiumUpdates: StateFlow<Boolean> = _premiumUpdates.asStateFlow()

    override suspend fun isRemoveAdsPurchased(): Boolean {
        return settingsDataStore.isPremiumRemoveAds()
    }

    override suspend fun launchRemoveAdsPurchase(activity: Activity): Boolean {
        Log.d(TAG, "Fake purchase flow: granting Remove Ads (replace with Play Billing)")
        _premiumUpdates.value = true
        return true
    }

    override suspend fun restorePurchases(): Boolean {
        val owned = settingsDataStore.isPremiumRemoveAds()
        if (owned) _premiumUpdates.value = true
        return owned
    }

    override suspend fun hasPendingRemoveAdsPurchase(): Boolean = false

    override suspend fun getRemoveAdsPrice(): String? = null
}
