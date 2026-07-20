package com.example.videomax.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.ThemeMode
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.usecase.ObserveSettingsUseCase
import com.example.videomax.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
	observeSettings: ObserveSettingsUseCase,
	private val settingsRepository: SettingsRepository
) : ViewModel() {
	val settings: StateFlow<AppSettings> = observeSettings()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

	fun setTheme(mode: ThemeMode) {
		viewModelScope.launch { settingsRepository.setThemeMode(mode) }
	}

	fun setSpeed(speed: Float) {
		viewModelScope.launch { settingsRepository.setDefaultPlaybackSpeed(speed) }
	}

	fun setRememberPosition(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setRememberPlaybackPosition(enabled) }
	}

	fun setAutoPlayNext(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setAutoPlayNext(enabled) }
	}

	fun setSeekStep(seconds: Int) {
		viewModelScope.launch { settingsRepository.setSeekStepSeconds(seconds) }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel()
) {
	val settings by viewModel.settings.collectAsStateWithLifecycle()

	Scaffold(
		topBar = { TopAppBar(title = { Text("Settings") }) }
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
		) {
			Text(
				text = "Appearance",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
			)
			SingleChoiceSegmentedButtonRow(
				modifier = Modifier.padding(horizontal = 16.dp)
			) {
				ThemeMode.entries.forEachIndexed { index, mode ->
					SegmentedButton(
						selected = settings.themeMode == mode,
						onClick = { viewModel.setTheme(mode) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = ThemeMode.entries.size
						),
						label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
					)
				}
			}

			Text(
				text = "Playback",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
			)
			ListItem(
				headlineContent = { Text("Default speed ${Formatters.formatSpeed(settings.defaultPlaybackSpeed)}") },
				supportingContent = {
					Slider(
						value = settings.defaultPlaybackSpeed,
						onValueChange = viewModel::setSpeed,
						valueRange = 0.25f..2.5f,
						steps = 8
					)
				}
			)
			ListItem(
				headlineContent = { Text("Seek step: ${settings.seekStepSeconds}s") },
				supportingContent = {
					Slider(
						value = settings.seekStepSeconds.toFloat(),
						onValueChange = { viewModel.setSeekStep(it.toInt()) },
						valueRange = 5f..30f,
						steps = 4
					)
				}
			)
			ListItem(
				headlineContent = { Text("Remember playback position") },
				trailingContent = {
					Switch(
						checked = settings.rememberPlaybackPosition,
						onCheckedChange = viewModel::setRememberPosition
					)
				}
			)
			ListItem(
				headlineContent = { Text("Auto-play next in folder") },
				trailingContent = {
					Switch(
						checked = settings.autoPlayNext,
						onCheckedChange = viewModel::setAutoPlayNext
					)
				}
			)

			Text(
				text = "About",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
			)
			ListItem(
				headlineContent = { Text("videomax") },
				supportingContent = {
					Text("Media3 ExoPlayer · Material Design 3 · Clean Architecture")
				}
			)
		}
	}
}
