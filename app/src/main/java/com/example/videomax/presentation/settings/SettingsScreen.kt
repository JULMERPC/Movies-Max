package com.example.videomax.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.R
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

	fun setShowHiddenFiles(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setShowHiddenFiles(enabled) }
	}

	fun setGesturesEnabled(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setGesturesEnabled(enabled) }
	}

	fun setAutoPip(enabled: Boolean) {
		viewModelScope.launch { settingsRepository.setAutoPip(enabled) }
	}

	fun addBlacklist(entry: String) {
		viewModelScope.launch { settingsRepository.addBlacklistEntry(entry) }
	}

	fun removeBlacklist(entry: String) {
		viewModelScope.launch { settingsRepository.removeBlacklistEntry(entry) }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel()
) {
	val settings by viewModel.settings.collectAsStateWithLifecycle()
	var showBlacklistDialog by remember { mutableStateOf(false) }
	var blacklistInput by remember { mutableStateOf("") }

	val gradient = Brush.verticalGradient(
		colors = listOf(
			MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
			MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
			MaterialTheme.colorScheme.background
		)
	)

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.settings_title)) },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
					titleContentColor = MaterialTheme.colorScheme.onSurface
				)
			)
		}
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(gradient)
				.padding(padding)
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 16.dp, vertical = 8.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				GlassSection(title = stringResource(R.string.settings_appearance)) {
					ThemeDropdown(
						selected = settings.themeMode,
						onSelected = viewModel::setTheme
					)
				}

				GlassSection(title = stringResource(R.string.settings_playback)) {
					GlassSwitchRow(
						title = stringResource(R.string.settings_remember_position),
						checked = settings.rememberPlaybackPosition,
						onCheckedChange = viewModel::setRememberPosition
					)
					GlassSwitchRow(
						title = stringResource(R.string.settings_autoplay_next),
						checked = settings.autoPlayNext,
						onCheckedChange = viewModel::setAutoPlayNext
					)
					Text(
						text = stringResource(
							R.string.settings_speed,
							Formatters.formatSpeed(settings.defaultPlaybackSpeed)
						),
						style = MaterialTheme.typography.bodyMedium
					)
					Slider(
						value = settings.defaultPlaybackSpeed,
						onValueChange = viewModel::setSpeed,
						valueRange = 0.25f..2.5f,
						steps = 8
					)
					Text(
						text = stringResource(R.string.settings_seek_step, settings.seekStepSeconds),
						style = MaterialTheme.typography.bodyMedium
					)
					Slider(
						value = settings.seekStepSeconds.toFloat(),
						onValueChange = { viewModel.setSeekStep(it.toInt()) },
						valueRange = 5f..30f,
						steps = 4
					)
				}

				GlassSection(title = stringResource(R.string.settings_player), icon = Icons.Default.Gesture) {
					GlassSwitchRow(
						title = stringResource(R.string.settings_gestures),
						subtitle = stringResource(R.string.settings_gestures_desc),
						icon = Icons.Default.Gesture,
						checked = settings.gesturesEnabled,
						onCheckedChange = viewModel::setGesturesEnabled
					)
					GlassSwitchRow(
						title = stringResource(R.string.settings_auto_pip),
						subtitle = stringResource(R.string.settings_auto_pip_desc),
						icon = Icons.Default.PictureInPictureAlt,
						checked = settings.autoPip,
						onCheckedChange = viewModel::setAutoPip
					)
				}

				GlassSection(title = stringResource(R.string.settings_library), icon = Icons.Default.Visibility) {
					GlassSwitchRow(
						title = stringResource(R.string.settings_hidden_files),
						subtitle = stringResource(R.string.settings_hidden_files_desc),
						icon = Icons.Default.Visibility,
						checked = settings.showHiddenFiles,
						onCheckedChange = viewModel::setShowHiddenFiles
					)
				}

				GlassSection(title = stringResource(R.string.settings_blacklist), icon = Icons.Default.Block) {
					Text(
						text = stringResource(R.string.settings_blacklist_desc),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(8.dp))
					TextButton(onClick = { showBlacklistDialog = true }) {
						Icon(Icons.Default.Add, contentDescription = null)
						Spacer(modifier = Modifier.padding(4.dp))
						Text(stringResource(R.string.settings_blacklist_add))
					}
					if (settings.blacklist.isEmpty()) {
						Text(
							text = stringResource(R.string.settings_blacklist_empty),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					} else {
						settings.blacklist.forEach { entry ->
							ListItem(
								headlineContent = { Text(entry) },
								trailingContent = {
									IconButton(onClick = { viewModel.removeBlacklist(entry) }) {
										Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete))
									}
								},
								colors = ListItemDefaults.colors(containerColor = Color.Transparent)
							)
						}
					}
				}

				GlassSection(title = stringResource(R.string.settings_about)) {
					Text("videomax", style = MaterialTheme.typography.titleMedium)
					Text(
						text = stringResource(R.string.settings_about_desc),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				Spacer(modifier = Modifier.height(24.dp))
			}
		}
	}

	if (showBlacklistDialog) {
		AlertDialog(
			onDismissRequest = { showBlacklistDialog = false },
			title = { Text(stringResource(R.string.settings_blacklist_add)) },
			text = {
				OutlinedTextField(
					value = blacklistInput,
					onValueChange = { blacklistInput = it },
					label = { Text(stringResource(R.string.settings_blacklist_hint)) },
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						viewModel.addBlacklist(blacklistInput)
						blacklistInput = ""
						showBlacklistDialog = false
					}
				) { Text(stringResource(R.string.add)) }
			},
			dismissButton = {
				TextButton(onClick = { showBlacklistDialog = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
			containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
	selected: ThemeMode,
	onSelected: (ThemeMode) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }

	val label = when (selected) {
		ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
		ThemeMode.LIGHT -> stringResource(R.string.theme_light)
		ThemeMode.DARK -> stringResource(R.string.theme_dark)
	}

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = { expanded = it }
	) {
		OutlinedTextField(
			value = label,
			onValueChange = {},
			readOnly = true,
			label = { Text(stringResource(R.string.settings_theme)) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			modifier = Modifier
				.fillMaxWidth()
				.menuAnchor(MenuAnchorType.PrimaryNotEditable),
			colors = OutlinedTextFieldDefaults.colors(
				unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
				focusedTextColor = MaterialTheme.colorScheme.onSurface
			)
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false }
		) {
			ThemeMode.entries.forEach { mode ->
				val modeLabel = when (mode) {
					ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
					ThemeMode.LIGHT -> stringResource(R.string.theme_light)
					ThemeMode.DARK -> stringResource(R.string.theme_dark)
				}
				DropdownMenuItem(
					text = {
						Text(
							text = modeLabel,
							color = if (mode == selected) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.onSurface
						)
					},
					onClick = {
						onSelected(mode)
						expanded = false
					}
				)
			}
		}
	}
}

@Composable
private fun GlassSection(
	title: String,
	icon: ImageVector? = null,
	content: @Composable () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(24.dp))
			.border(
				width = 1.dp,
				brush = Brush.linearGradient(
					listOf(
						Color.White.copy(alpha = 0.35f),
						MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
					)
				),
				shape = RoundedCornerShape(24.dp)
			),
		color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
		tonalElevation = 0.dp,
		shadowElevation = 0.dp,
		shape = RoundedCornerShape(24.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				if (icon != null) {
					Icon(
						imageVector = icon,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary
					)
					Spacer(modifier = Modifier.padding(6.dp))
				}
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary
				)
			}
			Spacer(modifier = Modifier.height(12.dp))
			content()
		}
	}
}

@Composable
private fun GlassSwitchRow(
	title: String,
	subtitle: String? = null,
	icon: ImageVector? = null,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	ListItem(
		headlineContent = { Text(title) },
		supportingContent = subtitle?.let { { Text(it) } },
		leadingContent = icon?.let {
			{
				Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
			}
		},
		trailingContent = {
			Switch(checked = checked, onCheckedChange = onCheckedChange)
		},
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
}
