package com.puma.videomax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.puma.videomax.ads.AdsManager
import com.puma.videomax.ads.ConsentManager
import com.puma.videomax.ads.LocalConsentManager
import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.presentation.firstlaunch.FirstLaunchScreen
import com.puma.videomax.presentation.navigation.VideoPlayerNavHost
import com.puma.videomax.presentation.permissions.MediaPermissionGate
import com.puma.videomax.presentation.permissions.hasMediaPermission
import com.puma.videomax.presentation.permissions.requiredMediaPermissions
import com.puma.videomax.presentation.theme.VideoPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var adsManager: AdsManager

    private var hasPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPermission = hasMediaPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPermission = hasMediaPermission(this)

        val settingsState = settingsRepository.settings.stateIn(
            lifecycleScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings()
        )

        setContent {
            val settings by settingsState.collectAsStateWithLifecycle()
            val showFirstLaunch = !settings.termsAccepted

            VideoPlayerProTheme(themeMode = settings.themeMode, accentColor = settings.accentColor) {
                CompositionLocalProvider(LocalConsentManager provides consentManager) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
						if (showFirstLaunch) {
							FirstLaunchScreen(
								selectedAccentColor = settings.accentColor,
								onColorSelected = { color ->
									lifecycleScope.launch {
										settingsRepository.setAccentColor(color)
									}
								},
								onAccept = {
                                    lifecycleScope.launch {
                                        settingsRepository.setTermsAccepted(true)
                                    }
                                    consentManager.requestConsentInfoUpdate(this@MainActivity) {
                                        consentManager.loadAndShowConsentFormIfRequired(this@MainActivity) {
                                            adsManager.onConsentReady()
                                        }
                                    }
                                }
                            )
                        } else {
                            MediaPermissionGate(
                                hasPermission = hasPermission,
                                onRequestPermission = {
                                    permissionLauncher.launch(requiredMediaPermissions())
                                }
                            ) {
                                VideoPlayerNavHost()
                            }
                        }
                    }
                }
            }
        }
    }
}
