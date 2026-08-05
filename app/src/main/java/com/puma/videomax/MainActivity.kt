package com.puma.videomax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.puma.videomax.domain.model.AppSettings
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.presentation.navigation.VideoPlayerNavHost
import com.puma.videomax.presentation.permissions.MediaPermissionGate
import com.puma.videomax.presentation.permissions.hasMediaPermission
import com.puma.videomax.presentation.permissions.requiredMediaPermissions
import com.puma.videomax.presentation.theme.VideoPlayerProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	@Inject
	lateinit var settingsRepository: SettingsRepository

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

			VideoPlayerProTheme(themeMode = settings.themeMode, accentColor = settings.accentColor) {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background
				) {
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
