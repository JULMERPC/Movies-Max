package com.puma.videomax.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.LocalActivity
import com.puma.videomax.R
import com.puma.videomax.ads.LocalConsentManager
import com.puma.videomax.domain.model.ThemeMode
import com.puma.videomax.util.Formatters
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel(),
	onOpenPrivateFolder: () -> Unit = {},
	onOpenSupportDeveloper: () -> Unit = {}
) {
	val settings by viewModel.settings.collectAsStateWithLifecycle()
	val consentManager = LocalConsentManager.current
	val activity = LocalActivity.current

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenGradient())
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			TopAppBar(
				title = {
					Text(
						stringResource(R.string.settings_title),
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold,
						color = VideoMaxTheme.extended.textPrimary
					)
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent
				)
			)

			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(horizontal = VideoMaxDimens.spacingLg),
				verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd)
			) {
				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXs))

				SettingsCard(
					title = "Apoya al desarrollador",
					icon = Icons.Default.Favorite
				) {
					SettingsClickableRow(
						title = "Apoya el desarrollo de Jualix",
						subtitle = "Viendo un anuncio voluntario",
						icon = Icons.Default.Favorite,
						onClick = onOpenSupportDeveloper
					)
				}

				SettingsCard(
					title = stringResource(R.string.settings_appearance),
					icon = Icons.Default.Gesture
				) {
					ThemeDropdown(
						selected = settings.themeMode,
						onSelected = viewModel::setTheme
					)
					Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
					ColorPickerRow(
						selectedColor = settings.accentColor,
						onColorSelected = viewModel::setAccentColor
					)
				}

				SettingsCard(
					title = stringResource(R.string.settings_playback),
					icon = Icons.Default.Speed
				) {
					SettingsSwitchRow(
						title = stringResource(R.string.settings_remember_position),
						checked = settings.rememberPlaybackPosition,
						onCheckedChange = viewModel::setRememberPosition
					)
					SettingsSwitchRow(
						title = stringResource(R.string.settings_autoplay_next),
						checked = settings.autoPlayNext,
						onCheckedChange = viewModel::setAutoPlayNext
					)
					Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
					SettingsSliderRow(
						label = stringResource(R.string.settings_speed, Formatters.formatSpeed(settings.defaultPlaybackSpeed)),
						value = settings.defaultPlaybackSpeed,
						valueRange = 0.25f..2.5f,
						steps = 8,
						onValueChange = viewModel::setSpeed
					)
					SettingsSliderRow(
						label = stringResource(R.string.settings_seek_step, settings.seekStepSeconds),
						value = settings.seekStepSeconds.toFloat(),
						valueRange = 5f..30f,
						steps = 4,
						onValueChange = { viewModel.setSeekStep(it.toInt()) }
					)
				}

				SettingsCard(
					title = stringResource(R.string.settings_player),
					icon = Icons.Default.Gesture
				) {
					SettingsSwitchRow(
						title = stringResource(R.string.settings_gestures),
						subtitle = stringResource(R.string.settings_gestures_desc),
						checked = settings.gesturesEnabled,
						onCheckedChange = viewModel::setGesturesEnabled
					)
					SettingsSwitchRow(
						title = stringResource(R.string.settings_auto_pip),
						subtitle = stringResource(R.string.settings_auto_pip_desc),
						checked = settings.autoPip,
						onCheckedChange = viewModel::setAutoPip
					)
				}

				SettingsCard(
					title = "Archivos",
					icon = Icons.Default.Folder
				) {
					SettingsSwitchRow(
						title = "Mostrar archivos .nomedia",
						subtitle = "Incluir carpetas marcadas con .nomedia",
						checked = settings.showNomedia,
						onCheckedChange = viewModel::setShowNomedia
					)
					SettingsSwitchRow(
						title = "Mostrar archivos ocultos",
						subtitle = "Incluir archivos y carpetas ocultos",
						checked = settings.showHiddenFiles,
						onCheckedChange = viewModel::setShowHiddenFiles
					)
				}

			SettingsCard(
				title = "Carpeta privada",
				icon = Icons.Default.Lock
			) {
				SettingsClickableRow(
					title = if (settings.privateFolderPin != null) "Carpeta privada activa" else "Configurar carpeta privada",
					subtitle = if (settings.privateFolderPin != null) "${settings.privateVideoIds.size} videos protegidos" else "Proteger videos con PIN de 4 dígitos",
					icon = Icons.Default.Lock,
					onClick = onOpenPrivateFolder
				)
			}

			SettingsCard(
				title = "Privacidad",
				icon = Icons.Default.Lock
			) {
				SettingsClickableRow(
					title = "Configuración de privacidad",
					subtitle = "Administrar tus preferencias de consentimiento",
					icon = Icons.Default.Lock,
					onClick = { activity?.let { consentManager.showPrivacyOptionsForm(it) } }
				)
			}

			SettingsCard(
				title = stringResource(R.string.settings_about),
					icon = Icons.Default.Info
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically
					) {
						Column(modifier = Modifier.weight(1f)) {
							Text(
								"Jualix",
								style = MaterialTheme.typography.titleMedium,
								color = VideoMaxTheme.extended.textPrimary
							)
							Text(
								text = stringResource(R.string.settings_about_desc),
								style = MaterialTheme.typography.bodySmall,
								color = VideoMaxTheme.extended.textTertiary
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxxl))
			}
		}
	}
}

@Composable
private fun SettingsCard(
	title: String,
	icon: ImageVector,
	content: @Composable () -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(VideoMaxDimens.radiusLg),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
	) {
		Column(
			modifier = Modifier.padding(VideoMaxDimens.spacingLg)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.padding(bottom = VideoMaxDimens.spacingMd)
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(20.dp)
				)
				Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
				Text(
					text = title,
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary
				)
			}
			content()
		}
	}
}

@Composable
private fun SettingsSwitchRow(
	title: String,
	subtitle: String? = null,
	checked: Boolean = false,
	onCheckedChange: ((Boolean) -> Unit)? = null
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = VideoMaxDimens.spacingXs),
		verticalAlignment = Alignment.CenterVertically
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				title,
				style = MaterialTheme.typography.bodyLarge,
				color = VideoMaxTheme.extended.textPrimary
			)
			if (subtitle != null) {
				Text(
					subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = VideoMaxTheme.extended.textTertiary
				)
			}
		}
		Switch(
			checked = checked,
			onCheckedChange = onCheckedChange ?: {},
			colors = SwitchDefaults.colors(
				checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
				checkedTrackColor = MaterialTheme.colorScheme.primary
			)
		)
	}
}

@Composable
private fun SettingsClickableRow(
	title: String,
	subtitle: String? = null,
	icon: ImageVector,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
			.clickable(onClick = onClick)
			.padding(vertical = VideoMaxDimens.spacingSm, horizontal = VideoMaxDimens.spacingXs),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(20.dp)
		)
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				title,
				style = MaterialTheme.typography.bodyLarge,
				color = VideoMaxTheme.extended.textPrimary
			)
			if (subtitle != null) {
				Text(
					subtitle,
					style = MaterialTheme.typography.bodySmall,
					color = VideoMaxTheme.extended.textTertiary
				)
			}
		}
		Icon(
			imageVector = Icons.Default.ChevronRight,
			contentDescription = null,
			tint = VideoMaxTheme.extended.textTertiary,
			modifier = Modifier.size(20.dp)
		)
	}
}

@Composable
private fun SettingsSliderRow(
	label: String,
	value: Float,
	valueRange: ClosedFloatingPointRange<Float>,
	steps: Int,
	onValueChange: (Float) -> Unit
) {
	Column(modifier = Modifier.padding(vertical = VideoMaxDimens.spacingXs)) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			color = VideoMaxTheme.extended.textPrimary
		)
		Slider(
			value = value,
			onValueChange = onValueChange,
			valueRange = valueRange,
			steps = steps,
			colors = SliderDefaults.colors(
				thumbColor = MaterialTheme.colorScheme.primary,
				activeTrackColor = MaterialTheme.colorScheme.primary,
				inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
			)
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
		ThemeMode.AMOLED -> stringResource(R.string.theme_amoled)
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
			shape = RoundedCornerShape(VideoMaxDimens.radiusMd),
			colors = OutlinedTextFieldDefaults.colors(
				unfocusedTextColor = VideoMaxTheme.extended.textPrimary,
				focusedTextColor = VideoMaxTheme.extended.textPrimary
			)
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false },
			shape = RoundedCornerShape(VideoMaxDimens.radiusMd),
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
		) {
			ThemeMode.entries.forEach { mode ->
				val modeLabel = when (mode) {
					ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
					ThemeMode.LIGHT -> stringResource(R.string.theme_light)
					ThemeMode.DARK -> stringResource(R.string.theme_dark)
					ThemeMode.AMOLED -> stringResource(R.string.theme_amoled)
				}
				DropdownMenuItem(
					text = {
						Text(
							text = modeLabel,
							color = if (mode == selected) MaterialTheme.colorScheme.primary
							else VideoMaxTheme.extended.textPrimary
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
private fun ColorPickerRow(
	selectedColor: Long,
	onColorSelected: (Long) -> Unit
) {
	var showPicker by remember { mutableStateOf(false) }

	val presetColors = listOf(
		0xFFFFFFFFL,
		0xFF006B5EL,
		0xFFE91E63L,
		0xFF2196F3L,
		0xFFFF9800L,
		0xFF9C27B0L
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
			.clickable { showPicker = true }
			.padding(vertical = VideoMaxDimens.spacingSm, horizontal = VideoMaxDimens.spacingXs),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			"Tu color favorito",
			style = MaterialTheme.typography.bodyLarge,
			color = VideoMaxTheme.extended.textPrimary,
			modifier = Modifier.weight(1f)
		)
		Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
			presetColors.take(4).forEach { color ->
				Box(
					modifier = Modifier
						.size(24.dp)
						.clip(CircleShape)
						.background(Color(color.toInt()))
						.then(
							if (selectedColor == color) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
							else if (color == 0xFFFFFFFFL) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
							else Modifier
						)
				)
			}
			if (selectedColor != 0L && selectedColor !in presetColors.take(4)) {
				Box(
					modifier = Modifier
						.size(24.dp)
						.clip(CircleShape)
						.background(Color(selectedColor.toInt()))
						.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
				)
			}
		}
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
		Icon(
			imageVector = Icons.Default.ChevronRight,
			contentDescription = null,
			tint = VideoMaxTheme.extended.textTertiary,
			modifier = Modifier.size(20.dp)
		)
	}

	if (showPicker) {
		AlertDialog(
			onDismissRequest = { showPicker = false },
			title = { Text("Elegí tu color") },
			text = {
				Column {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.height(44.dp)
							.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
							.background(MaterialTheme.colorScheme.primary)
							.clickable {
								onColorSelected(0L)
								showPicker = false
							},
						contentAlignment = Alignment.Center
					) {
						Text("Por defecto", color = MaterialTheme.colorScheme.onPrimary)
					}
					Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))
					LazyVerticalGrid(
						columns = GridCells.Fixed(4),
						horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm),
						verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm)
					) {
						items(presetColors) { color ->
							val isSelected = selectedColor == color
							Box(
								modifier = Modifier
									.aspectRatio(1f)
									.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
									.background(Color(color.toInt()))
									.then(
										if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(VideoMaxDimens.radiusSm))
										else if (color == 0xFFFFFFFFL) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(VideoMaxDimens.radiusSm))
										else Modifier
									)
									.clickable {
										onColorSelected(color)
										showPicker = false
									}
							)
						}
					}
				}
			},
			confirmButton = {},
			dismissButton = {
				TextButton(onClick = { showPicker = false }) {
					Text("Cerrar")
				}
			}
		)
	}
}
