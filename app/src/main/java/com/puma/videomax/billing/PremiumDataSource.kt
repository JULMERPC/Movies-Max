package com.puma.videomax.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the IAP backend so the monetization logic never depends
 * directly on Play Billing.
 *
 * Implementations notify purchases through [premiumUpdates]; the
 * [com.puma.videomax.domain.monetization.MonetizationRepository] collects
 * that flow and persists the Premium flag. This one-way direction avoids a
 * dependency cycle (repository → data source only).
 */
interface PremiumDataSource {

    /** Emits true once "Remove Ads" is owned. Never completes. */
    val premiumUpdates: StateFlow<Boolean>

    suspend fun isRemoveAdsPurchased(): Boolean

    suspend fun launchRemoveAdsPurchase(activity: Activity): Boolean

    /**
     * Re-queries owned INAPP purchases and emits Premium when
     * "remove_ads" is owned (PURCHASED). Used at startup/reconnect and
     * when Play answers ITEM_ALREADY_OWNED instead of opening a new payment.
     */
    suspend fun restorePurchases(): Boolean

    /**
     * True when Play reports "remove_ads" in PENDING state (e.g. cash payment
     * not completed yet). A pending purchase blocks repurchase with
     * ITEM_ALREADY_OWNED but grants NO entitlement: the UI must tell the user
     * to finish the payment in Play instead of showing a generic error.
     */
    suspend fun hasPendingRemoveAdsPurchase(): Boolean

    /** Localized price ("US$4.99") or null while unavailable. */
    suspend fun getRemoveAdsPrice(): String?
}
