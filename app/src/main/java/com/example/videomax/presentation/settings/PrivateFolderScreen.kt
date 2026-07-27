package com.example.videomax.presentation.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.videomax.presentation.theme.screenGradient
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.example.videomax.domain.model.AppSettings
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PrivateFolderViewModel @Inject constructor(
	observeSettings: ObserveSettingsUseCase,
	private val settingsRepository: SettingsRepository,
	private val videoRepository: VideoRepository
) : ViewModel() {

	val settings: StateFlow<AppSettings> = observeSettings()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

	private val _privateVideos = MutableStateFlow<List<Video>>(emptyList())
	val privateVideos: StateFlow<List<Video>> = _privateVideos

	init {
		viewModelScope.launch {
			settings.collect { s ->
				if (s.privateVideoIds.isNotEmpty()) {
					val vids = withContext(Dispatchers.IO) {
						videoRepository.getVideosByIds(s.privateVideoIds)
					}
					_privateVideos.value = vids
				} else {
					_privateVideos.value = emptyList()
				}
			}
		}
	}

	fun setPin(pin: String) {
		viewModelScope.launch { settingsRepository.setPrivateFolderPin(pin) }
	}

	fun removePin() {
		viewModelScope.launch {
			settingsRepository.setPrivateFolderPin(null)
			settingsRepository.setPrivateVideoIds(emptyList())
		}
	}

	fun addToPrivate(videoId: Long) {
		viewModelScope.launch {
			val current = settings.value.privateVideoIds
			if (videoId !in current) {
				settingsRepository.setPrivateVideoIds(current + videoId)
			}
		}
	}

	fun removeFromPrivate(videoId: Long) {
		viewModelScope.launch {
			val current = settings.value.privateVideoIds
			settingsRepository.setPrivateVideoIds(current - videoId)
		}
	}

	suspend fun getAllVideos(): List<Video> = withContext(Dispatchers.IO) {
		val ids = videoRepository.getVideoIds("", SortOption.DATE_DESC, null)
		videoRepository.getVideosByIds(ids)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateFolderScreen(
	onBack: () -> Unit,
	viewModel: PrivateFolderViewModel = hiltViewModel()
) {
	val settings by viewModel.settings.collectAsStateWithLifecycle()
	val privateVideos by viewModel.privateVideos.collectAsStateWithLifecycle()
	var isUnlocked by remember { mutableStateOf(false) }
	var showSetPin by remember { mutableStateOf(false) }
	var showAddVideos by remember { mutableStateOf(false) }

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text("Carpeta privada") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
					}
				},
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
			when {
				settings.privateFolderPin == null -> {
					SetPinContent(
						onSetPin = { pin ->
							viewModel.setPin(pin)
							isUnlocked = true
						},
						onBack = onBack
					)
				}
				!isUnlocked -> {
					VerifyPinContent(
						pin = settings.privateFolderPin!!,
						onVerified = { isUnlocked = true },
						onBack = onBack
					)
				}
				else -> {
					PrivateFolderContent(
						privateVideos = privateVideos,
						onRemove = viewModel::removeFromPrivate,
						onAdd = { showAddVideos = true },
						onChangePin = { showSetPin = true },
						onRemovePin = { viewModel.removePin(); isUnlocked = false }
					)
				}
			}
		}
	}

	if (showSetPin) {
		SetPinDialog(
			onSetPin = { pin ->
				viewModel.setPin(pin)
				showSetPin = false
				isUnlocked = true
			},
			onDismiss = { showSetPin = false }
		)
	}

	if (showAddVideos) {
		AddVideosDialog(
			existingIds = settings.privateVideoIds,
			onAdd = { videoId ->
				viewModel.addToPrivate(videoId)
			},
			onDismiss = { showAddVideos = false },
			viewModel = viewModel
		)
	}
}

@Composable
private fun SetPinContent(
	onSetPin: (String) -> Unit,
	onBack: () -> Unit
) {
	var pin by remember { mutableStateOf("") }
	var confirmPin by remember { mutableStateOf("") }
	var error by remember { mutableStateOf<String?>(null) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = Icons.Default.Lock,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(64.dp)
		)
		Spacer(modifier = Modifier.height(24.dp))
		Text(
			text = "Crear carpeta privada",
			style = MaterialTheme.typography.headlineSmall,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = "Ingresa un PIN de 4 dígitos para proteger tus videos",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(32.dp))

		OutlinedTextField(
			value = pin,
			onValueChange = { if (it.length <= 4) pin = it },
			label = { Text("PIN") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
			maxLines = 1,
			singleLine = true,
			modifier = Modifier.fillMaxWidth()
		)
		Spacer(modifier = Modifier.height(12.dp))

		OutlinedTextField(
			value = confirmPin,
			onValueChange = { if (it.length <= 4) confirmPin = it },
			label = { Text("Confirmar PIN") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
			maxLines = 1,
			singleLine = true,
			modifier = Modifier.fillMaxWidth()
		)

		if (error != null) {
			Spacer(modifier = Modifier.height(8.dp))
			Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
		}

		Spacer(modifier = Modifier.height(24.dp))

		TextButton(
			onClick = {
				when {
					pin.length != 4 -> error = "El PIN debe tener 4 dígitos"
					pin != confirmPin -> error = "Los PINs no coinciden"
					else -> onSetPin(pin)
				}
			},
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Crear carpeta privada")
		}

		Spacer(modifier = Modifier.height(8.dp))

		TextButton(onClick = onBack) {
			Text("Cancelar")
		}
	}
}

@Composable
private fun VerifyPinContent(
	pin: String,
	onVerified: () -> Unit,
	onBack: () -> Unit
) {
	var input by remember { mutableStateOf("") }
	var error by remember { mutableStateOf(false) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = Icons.Default.Lock,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(64.dp)
		)
		Spacer(modifier = Modifier.height(24.dp))
		Text(
			text = "Carpeta privada",
			style = MaterialTheme.typography.headlineSmall,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = "Ingresa el PIN para acceder",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(32.dp))

		OutlinedTextField(
			value = input,
			onValueChange = {
				if (it.length <= 4) input = it
				if (it.length == 4 && it == pin) onVerified()
				error = it.length == 4 && it != pin
			},
			label = { Text("PIN") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
			maxLines = 1,
			singleLine = true,
			isError = error,
			modifier = Modifier.fillMaxWidth()
		)

		if (error) {
			Spacer(modifier = Modifier.height(8.dp))
			Text(text = "PIN incorrecto", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
		}

		Spacer(modifier = Modifier.height(24.dp))

		TextButton(onClick = onBack) {
			Text("Cancelar")
		}
	}
}

@Composable
private fun PrivateFolderContent(
	privateVideos: List<Video>,
	onRemove: (Long) -> Unit,
	onAdd: () -> Unit,
	onChangePin: () -> Unit,
	onRemovePin: () -> Unit
) {
	var showRemovePinDialog by remember { mutableStateOf(false) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "${privateVideos.size} videos protegidos",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface
			)
			Row {
				TextButton(onClick = onChangePin) { Text("Cambiar PIN") }
				TextButton(onClick = { showRemovePinDialog = true }) { Text("Eliminar") }
			}
		}

		Spacer(modifier = Modifier.height(8.dp))


		Spacer(modifier = Modifier.height(16.dp))

		if (privateVideos.isEmpty()) {
			Box(
				modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
				contentAlignment = Alignment.Center
			) {
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Icon(
						imageVector = Icons.Default.Lock,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.size(48.dp)
					)
					Spacer(modifier = Modifier.height(12.dp))
					Text(
						text = "No hay videos en la carpeta privada",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		} else {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(120.dp),
				contentPadding = PaddingValues(4.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth()
			) {
				items(privateVideos, key = { it.id }) { video ->
					PrivateVideoCard(
						video = video,
						onRemove = { onRemove(video.id) }
					)
				}
			}
		}
	}

	if (showRemovePinDialog) {
		AlertDialog(
			onDismissRequest = { showRemovePinDialog = false },
			title = { Text("Eliminar carpeta privada") },
			text = { Text("Se eliminará el PIN y todos los videos saldrán de la carpeta privada. Los archivos no se borrarán del dispositivo.") },
			confirmButton = {
				TextButton(onClick = { onRemovePin(); showRemovePinDialog = false }) {
					Text("Eliminar", color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(onClick = { showRemovePinDialog = false }) { Text("Cancelar") }
			},
			containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
		)
	}
}

@Composable
private fun PrivateVideoCard(
	video: Video,
	onRemove: () -> Unit
) {
	val context = LocalContext.current

	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(8.dp),
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
	) {
		Column {
			Box {
				AsyncImage(
					model = ImageRequest.Builder(context)
						.data(video.uri)
						.decoderFactory(VideoFrameDecoder.Factory())
						.crossfade(true)
						.build(),
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(16f / 9f)
						.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
				)
				IconButton(
					onClick = onRemove,
					modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
				) {
					Icon(
						Icons.Default.Delete,
						contentDescription = "Remover",
						tint = MaterialTheme.colorScheme.error,
						modifier = Modifier.size(18.dp)
					)
				}
			}
			Text(
				text = video.displayName,
				style = MaterialTheme.typography.labelSmall,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
			)
		}
	}
}

@Composable
private fun SetPinDialog(
	onSetPin: (String) -> Unit,
	onDismiss: () -> Unit
) {
	var pin by remember { mutableStateOf("") }
	var confirmPin by remember { mutableStateOf("") }
	var error by remember { mutableStateOf<String?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Cambiar PIN") },
		text = {
			Column {
				OutlinedTextField(
					value = pin,
					onValueChange = { if (it.length <= 4) pin = it },
					label = { Text("Nuevo PIN") },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
					maxLines = 1,
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)
				Spacer(modifier = Modifier.height(8.dp))
				OutlinedTextField(
					value = confirmPin,
					onValueChange = { if (it.length <= 4) confirmPin = it },
					label = { Text("Confirmar PIN") },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
					maxLines = 1,
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)
				if (error != null) {
					Spacer(modifier = Modifier.height(4.dp))
					Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = {
				when {
					pin.length != 4 -> error = "Debe tener 4 dígitos"
					pin != confirmPin -> error = "No coinciden"
					else -> onSetPin(pin)
				}
			}) { Text("Guardar") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancelar") }
		},
		containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
	)
}

@Composable
private fun AddVideosDialog(
	existingIds: List<Long>,
	onAdd: (Long) -> Unit,
	onDismiss: () -> Unit,
	viewModel: PrivateFolderViewModel
) {
	var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
	LaunchedEffect(Unit) {
		videos = viewModel.getAllVideos()
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Agregar videos") },
		text = {
			if (videos.isEmpty()) {
				Text("No hay videos disponibles")
			} else {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(100.dp),
					contentPadding = PaddingValues(4.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp),
					horizontalArrangement = Arrangement.spacedBy(4.dp),
					modifier = Modifier.height(400.dp)
				) {
					items(videos, key = { it.id }) { video ->
						val isPrivate = video.id in existingIds
						Surface(
							modifier = Modifier
								.fillMaxWidth()
								.clickable { if (!isPrivate) onAdd(video.id) },
							shape = RoundedCornerShape(8.dp),
							color = if (isPrivate) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
							else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
						) {
							Column {
								Box(contentAlignment = Alignment.Center) {
									Text(
										text = video.displayName,
										style = MaterialTheme.typography.labelSmall,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										modifier = Modifier
											.fillMaxWidth()
											.aspectRatio(16f / 9f)
											.background(MaterialTheme.colorScheme.surfaceVariant)
											.padding(4.dp)
									)
									if (isPrivate) {
										Icon(
											Icons.Default.Check,
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary,
											modifier = Modifier.align(Alignment.Center)
										)
									}
								}
								Text(
									text = video.displayName,
									style = MaterialTheme.typography.labelSmall,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
									modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
								)
							}
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Listo") }
		},
		containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
	)
}
