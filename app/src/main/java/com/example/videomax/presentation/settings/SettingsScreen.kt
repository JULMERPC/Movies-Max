package com.example.videomax.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videomax.R
import com.example.videomax.domain.model.ThemeMode
import com.example.videomax.util.Formatters
import com.example.videomax.presentation.theme.VideoMaxDimens
import com.example.videomax.presentation.theme.VideoMaxTheme
import com.example.videomax.presentation.theme.screenGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel(),
	onOpenPrivateFolder: () -> Unit = {}
) {
	val settings by viewModel.settings.collectAsStateWithLifecycle()

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
	TopAppBar(
			title = { Text(stringResource(R.string.settings_title), color = VideoMaxTheme.extended.textPrimary) },
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = Color(
					MaterialTheme.colorScheme.primary.red,
					MaterialTheme.colorScheme.primary.green,
					MaterialTheme.colorScheme.primary.blue,
					0.10f
				).compositeOver(MaterialTheme.colorScheme.surface),
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
					.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
				verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg)
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
						style = MaterialTheme.typography.bodyMedium,
						color = VideoMaxTheme.extended.textPrimary
					)
					Slider(
						value = settings.defaultPlaybackSpeed,
						onValueChange = viewModel::setSpeed,
						valueRange = 0.25f..2.5f,
						steps = 8,
						colors = SliderDefaults.colors(
							thumbColor = MaterialTheme.colorScheme.primary,
							activeTrackColor = MaterialTheme.colorScheme.primary,
							inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
						)
					)
					Text(
						text = stringResource(R.string.settings_seek_step, settings.seekStepSeconds),
						style = MaterialTheme.typography.bodyMedium,
						color = VideoMaxTheme.extended.textPrimary
					)
					Slider(
						value = settings.seekStepSeconds.toFloat(),
						onValueChange = { viewModel.setSeekStep(it.toInt()) },
						valueRange = 5f..30f,
						steps = 4,
						colors = SliderDefaults.colors(
							thumbColor = MaterialTheme.colorScheme.primary,
							activeTrackColor = MaterialTheme.colorScheme.primary,
							inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
						)
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

				GlassSection(title = "Archivos", icon = Icons.Default.Folder) {
					GlassSwitchRow(
						title = "Mostrar archivos .nomedia",
						subtitle = "Incluir carpetas marcadas con .nomedia",
						icon = Icons.Default.HideImage,
						checked = settings.showNomedia,
						onCheckedChange = viewModel::setShowNomedia
					)
					GlassSwitchRow(
						title = "Mostrar archivos ocultos",
						subtitle = "Incluir archivos y carpetas ocultos",
						icon = Icons.Default.Visibility,
						checked = settings.showHiddenFiles,
						onCheckedChange = viewModel::setShowHiddenFiles
					)
				}

				GlassSection(title = "Carpeta privada", icon = Icons.Default.Lock) {
					GlassSwitchRow(
						title = if (settings.privateFolderPin != null) "Carpeta privada activa" else "Configurar carpeta privada",
						subtitle = if (settings.privateFolderPin != null) "${settings.privateVideoIds.size} videos protegidos" else "Proteger videos con PIN de 4 dígitos",
						icon = Icons.Default.Lock,
						checked = false,
						isToggle = false,
						onClick = onOpenPrivateFolder
					)
				}

				GlassSection(title = stringResource(R.string.settings_about)) {
					Text("videomax", style = MaterialTheme.typography.titleMedium, color = VideoMaxTheme.extended.textPrimary)
					Text(
						text = stringResource(R.string.settings_about_desc),
						style = MaterialTheme.typography.bodyMedium,
						color = VideoMaxTheme.extended.textTertiary
					)
				}

				Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))
			}
		}
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
			colors = OutlinedTextFieldDefaults.colors(
				unfocusedTextColor = VideoMaxTheme.extended.textPrimary,
				focusedTextColor = VideoMaxTheme.extended.textPrimary
			)
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false },
			shape = RoundedCornerShape(16.dp),
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
private fun GlassSection(
	title: String,
	icon: ImageVector? = null,
	content: @Composable () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusXl))
			.border(
				width = 1.dp,
				brush = Brush.linearGradient(
					listOf(
						Color.White.copy(alpha = 0.12f),
						MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
					)
				),
				shape = RoundedCornerShape(VideoMaxDimens.radiusXl)
			),
		color = MaterialTheme.colorScheme.surface.copy(alpha = VideoMaxDimens.alphaSurfaceGlassHover),
		tonalElevation = VideoMaxDimens.elevationNone,
		shadowElevation = VideoMaxDimens.elevationNone,
		shape = RoundedCornerShape(VideoMaxDimens.radiusXl)
	) {
		Column(modifier = Modifier.padding(VideoMaxDimens.spacingLg)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				if (icon != null) {
					Icon(
						imageVector = icon,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
					)
					Spacer(modifier = Modifier.padding(VideoMaxDimens.spacingXs))
				}
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary
				)
			}
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))
			content()
		}
	}
}

@Composable
private fun GlassSwitchRow(
	title: String,
	subtitle: String? = null,
	icon: ImageVector? = null,
	checked: Boolean = false,
	isToggle: Boolean = true,
	onCheckedChange: ((Boolean) -> Unit)? = null,
	onClick: (() -> Unit)? = null
) {
	ListItem(
		headlineContent = {
			Text(title, color = VideoMaxTheme.extended.textPrimary)
		},
		supportingContent = subtitle?.let {
			{ Text(it, color = VideoMaxTheme.extended.textTertiary) }
		},
		leadingContent = icon?.let {
			{
				Icon(
					it,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
				)
			}
		},
		trailingContent = {
			if (isToggle) {
				Switch(
					checked = checked,
					onCheckedChange = onCheckedChange ?: {},
					colors = SwitchDefaults.colors(
						checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
						checkedTrackColor = MaterialTheme.colorScheme.primary
					)
				)
			} else if (onClick != null) {
				Icon(
					imageVector = Icons.Default.ChevronRight,
					contentDescription = null,
					tint = VideoMaxTheme.extended.textTertiary,
					modifier = Modifier
						.size(VideoMaxDimens.iconSizeMd)
						.clickable { onClick() }
				)
			}
		},
		modifier = if (!isToggle && onClick != null) Modifier.clickable { onClick() } else Modifier,
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
}
