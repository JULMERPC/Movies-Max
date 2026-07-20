package com.example.videomax

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
import com.example.videomax.presentation.permissions.requiredMediaPermission
import com.example.videomax.presentation.theme.VideoPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.content.pm.PackageManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	@Inject
	lateinit var settingsRepository: SettingsRepository

	private var hasPermission by mutableStateOf(false)

	private val permissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		hasPermission = granted
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		hasPermission = ContextCompat.checkSelfPermission(
			this,
			requiredMediaPermission()
		) == PackageManager.PERMISSION_GRANTED

		val settingsState = settingsRepository.settings.stateIn(
			lifecycleScope,
			SharingStarted.WhileSubscribed(5_000),
			AppSettings()
		)

		setContent {
			val settings by settingsState.collectAsStateWithLifecycle()
			VideoPlayerProTheme(themeMode = settings.themeMode) {
				Surface(modifier = Modifier.fillMaxSize()) {
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

	override fun onPictureInPictureModeChanged(
		isInPictureInPictureMode: Boolean,
		newConfig: android.content.res.Configuration
	) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
	}
}
