package com.puma.videomax

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.puma.videomax.ads.AdsManager
import com.puma.videomax.ads.AppOpenAdManager
import com.puma.videomax.ads.ConsentManager
import com.puma.videomax.ads.LocalConsentManager
import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.presentation.navigation.VideoPlayerNavHost
import com.puma.videomax.presentation.permissions.MediaPermissionGate
import com.puma.videomax.presentation.permissions.hasMediaPermission
import com.puma.videomax.presentation.permissions.requiredMediaPermissions
import com.puma.videomax.presentation.theme.VideoPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    private var hasPermission by mutableStateOf(false)
    private var settingsLoaded by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPermission = hasMediaPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPermission = hasMediaPermission(this)
        Log.d(TAG, "onCreate - termsAccepted will be checked after settings load")

        val settingsState = settingsRepository.settings.stateIn(
            lifecycleScope,
            SharingStarted.Eagerly,
            AppSettings()
        )

        setContent {
            val settings by settingsState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                settingsRepository.settings.first()
                Log.d(TAG, "Settings loaded")
                settingsLoaded = true
            }

            VideoPlayerProTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalConsentManager provides consentManager) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!settingsLoaded) {
                            Log.d(TAG, "Waiting for settings to load...")
                        } else {
                            // Sin onboarding: directo al contenido. El consentimiento
                            // UMP sigue corriendo acá abajo como exige Google.
                            Log.d(TAG, "Starting consent and ad loading flow")
                            LaunchedEffect(Unit) {
                                Log.d(TAG, "Starting consent info update...")
                                consentManager.requestConsentInfoUpdate(this@MainActivity) {
                                    Log.d(TAG, "Consent info updated, loading consent form if required...")
                                    consentManager.loadAndShowConsentFormIfRequired(this@MainActivity) {
                                        Log.d(TAG, "Consent form dismissed, notifying ad managers...")
                                        lifecycleScope.launch {
                                            adsManager.onConsentReady()
                                            appOpenAdManager.onConsentReady()
                                            Log.d(TAG, "Consent flow completed")
                                        }
                                    }
                                }
                            }
                            MediaPermissionGate(
                                // POST_NOTIFICATIONS va en el mismo request que
                                // video/música (requiredMediaPermissions, API 33+).
                                hasPermission = hasPermission,
                                onRequestPermission = {
                                    permissionLauncher.launch(requiredMediaPermissions())
                                }
                            ) {
                                VideoPlayerNavHost(adsManager = adsManager)
                            }
                        }
                    }
                }
            }
        }
    }
}
