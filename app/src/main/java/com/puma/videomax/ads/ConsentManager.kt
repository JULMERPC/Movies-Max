package com.puma.videomax.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConsentManager"

@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val isDebuggable: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val _consentReady = MutableStateFlow(false)
    val consentReady: StateFlow<Boolean> = _consentReady.asStateFlow()

    private val params: ConsentRequestParameters by lazy {
        val builder = ConsentRequestParameters.Builder()
        if (isDebuggable) {
            val debugSettings = ConsentDebugSettings.Builder(context)
                .setDebugGeography(
                    ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
                )
                .build()
            builder.setConsentDebugSettings(debugSettings)
        }
        builder.build()
    }

    fun requestConsentInfoUpdate(
        activity: Activity,
        onReady: (() -> Unit)? = null
    ) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                Log.d(TAG, "Consent info updated")
                _canRequestAds.value = consentInformation.canRequestAds()
                _privacyOptionsRequired.value =
                    consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                _consentReady.value = true
                onReady?.invoke()
            },
            { requestConsentError ->
                Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                _canRequestAds.value = consentInformation.canRequestAds()
                _consentReady.value = true
                onReady?.invoke()
            }
        )
    }

    fun loadAndShowConsentFormIfRequired(
        activity: Activity,
        onDismissed: (() -> Unit)? = null
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Consent form error: ${formError.message}")
            }
            _canRequestAds.value = consentInformation.canRequestAds()
            _privacyOptionsRequired.value =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            onDismissed?.invoke()
        }
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissed: (() -> Unit)? = null
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.message}")
            }
            _canRequestAds.value = consentInformation.canRequestAds()
            onDismissed?.invoke()
        }
    }

    fun reset() {
        if (isDebuggable) {
            consentInformation.reset()
            _canRequestAds.value = false
            _privacyOptionsRequired.value = false
            _consentReady.value = false
        }
    }
}
