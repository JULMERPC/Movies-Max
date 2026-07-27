package com.example.videomax

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.presentation.navigation.VideoPlayerNavHost
import com.example.videomax.presentation.permissions.MediaPermissionGate
import com.example.videomax.presentation.permissions.hasMediaPermission
import com.example.videomax.presentation.permissions.requiredMediaPermission
import com.example.videomax.presentation.theme.VideoPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.videomax.domain.model.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	@Inject
	lateinit var settingsRepository: SettingsRepository

	private var hasPermission by mutableStateOf(false)

	private val permissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
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

			val isDark = when (settings.themeMode) {
				ThemeMode.LIGHT -> false
				ThemeMode.DARK -> true
				ThemeMode.SYSTEM -> isSystemInDarkTheme()
			}
			val backgroundGradient = Brush.verticalGradient(
				colors = if (isDark) {
					listOf(
						Color(0xFF0C1919), // Deep dark teal
						Color(0xFF050A0A)  // Solid near black
					)
				} else {
					listOf(
						Color(0xFFD9EFEF), // Beautiful soft teal
						Color(0xFFF0FDFD)  // Crisp white/teal mint
					)
				}
			)

			VideoPlayerProTheme(themeMode = settings.themeMode) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(backgroundGradient)
				) {
					MediaPermissionGate(
						hasPermission = hasPermission,
						onRequestPermission = {
							permissionLauncher.launch(requiredMediaPermission())
						}
					) {
						VideoPlayerNavHost()
					}
				}
			}
		}
	}

}
