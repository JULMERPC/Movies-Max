package com.puma.videomax.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puma.videomax.domain.model.PlaybackHistory
import com.puma.videomax.domain.model.Video
import com.puma.videomax.presentation.components.EmptyState
import com.puma.videomax.presentation.components.VideoGridItem
import com.puma.videomax.presentation.components.VideoThumbnail
import com.puma.videomax.presentation.navigation.Screen
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenGradient
import com.puma.videomax.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
	onOpenPlaylist: (Long) -> Unit,
	onOpenSmart: (String) -> Unit,
	viewModel: PlaylistsViewModel = hiltViewModel()
) {
	val playlists by viewModel.playlists.collectAsStateWithLifecycle()
	val favoritesCount by viewModel.favoritesCount.collectAsStateWithLifecycle()
	val historyCount by viewModel.historyCount.collectAsStateWithLifecycle()
	val mostPlayedCount by viewModel.mostPlayedCount.collectAsStateWithLifecycle()
	var showDialog by remember { mutableStateOf(false) }
	var name by remember { mutableStateOf("") }

	val smartCategories = listOf(
		SmartCategory(Screen.SmartCollection.FAVORITES, "Favoritos", "$favoritesCount videos", Icons.Default.Favorite),
		SmartCategory(Screen.SmartCollection.HISTORY, "Historial", "$historyCount vistos", Icons.Default.History),
		SmartCategory(Screen.SmartCollection.MOST_PLAYED, "Mas reproducidos", "$mostPlayedCount videos", Icons.Default.Whatshot),
		SmartCategory(Screen.SmartCollection.RECENT, "Vistos recientemente", "Seguir viendo", Icons.Default.History),
		SmartCategory(Screen.SmartCollection.QUEUE, "Cola de reproduccion", "Sesion actual", Icons.AutoMirrored.Filled.QueueMusic)
	)

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text("Playlists", color = VideoMaxTheme.extended.textPrimary) },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color(
						MaterialTheme.colorScheme.primary.red,
						MaterialTheme.colorScheme.primary.green,
						MaterialTheme.colorScheme.primary.blue,
						0.10f
					).compositeOver(MaterialTheme.colorScheme.surface)
				)
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = { showDialog = true }) {
				Icon(Icons.Default.Add, contentDescription = "Crear lista")
			}
		}
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(gradient)
				.padding(padding)
		) {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
				verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd)
			) {
				item {
					Text(
						text = "Colecciones inteligentes",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.primary
					)
				}
				items(smartCategories, key = { it.type }) { category ->
					SmartCategoryCard(category = category, onClick = { onOpenSmart(category.type) })
				}
				item {
					Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
					Text(
						text = "Tus listas",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.primary
					)
				}
				if (playlists.isEmpty()) {
					item {
						Text(
							text = "Creá una lista para organizar tus videos.",
							style = MaterialTheme.typography.bodyMedium,
							color = VideoMaxTheme.extended.textTertiary
						)
					}
				} else {
					items(playlists, key = { it.id }) { playlist ->
						ListItem(
							headlineContent = { Text(playlist.name, color = VideoMaxTheme.extended.textPrimary) },
							supportingContent = { Text("${playlist.videoCount} videos", color = VideoMaxTheme.extended.textTertiary) },
							leadingContent = {
								Icon(
									Icons.AutoMirrored.Filled.PlaylistPlay,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.primary,
									modifier = Modifier.size(VideoMaxDimens.iconSizeMd)
								)
							},
							trailingContent = {
								IconButton(onClick = { viewModel.delete(playlist.id) }) {
									Icon(
										Icons.Default.Delete,
										contentDescription = "Eliminar",
										tint = VideoMaxTheme.extended.textTertiary,
										modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
									)
								}
							},
							modifier = Modifier
								.fillMaxWidth()
								.clickable { onOpenPlaylist(playlist.id) },
							colors = ListItemDefaults.colors(containerColor = Color.Transparent)
						)
					}
				}
			}
		}
	}

	if (showDialog) {
		AlertDialog(
			onDismissRequest = { showDialog = false },
			title = { Text("Nueva lista") },
			text = {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text("Nombre") },
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if (name.isNotBlank()) {
							viewModel.create(name)
							name = ""
							showDialog = false
						}
					}
				) { Text("Crear") }
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
			},
			containerColor = MaterialTheme.colorScheme.surface
		)
	}
}

@Composable
private fun SmartCategoryCard(category: SmartCategory, onClick: () -> Unit) {
	Card(
		onClick = onClick,
		shape = RoundedCornerShape(VideoMaxDimens.radiusLg),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
		),
		elevation = CardDefaults.cardElevation(defaultElevation = VideoMaxDimens.elevationNone)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(VideoMaxDimens.spacingLg),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				imageVector = category.icon,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(VideoMaxDimens.iconSizeLg)
			)
			Spacer(modifier = Modifier.width(VideoMaxDimens.spacingLg))
			Column(modifier = Modifier.weight(1f)) {
				Text(category.title, style = MaterialTheme.typography.titleMedium, color = VideoMaxTheme.extended.textPrimary)
				Text(
					category.subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = VideoMaxTheme.extended.textTertiary
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCollectionScreen(
	onBack: () -> Unit,
	onOpenPlayer: (Long) -> Unit,
	viewModel: SmartCollectionViewModel = hiltViewModel()
) {
	val title = when (viewModel.type) {
		Screen.SmartCollection.FAVORITES -> "Favoritos"
		Screen.SmartCollection.HISTORY -> "Historial"
		Screen.SmartCollection.MOST_PLAYED -> "Mas reproducidos"
		Screen.SmartCollection.RECENT -> "Vistos recientemente"
		Screen.SmartCollection.QUEUE -> "Cola de reproduccion"
		else -> "Coleccion"
	}

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text(title, color = VideoMaxTheme.extended.textPrimary) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = VideoMaxTheme.extended.textPrimary)
					}
				},
				actions = {
					if (viewModel.type == Screen.SmartCollection.HISTORY ||
						viewModel.type == Screen.SmartCollection.RECENT
					) {
						IconButton(onClick = viewModel::clearHistory) {
							Icon(
								Icons.Default.Delete,
								contentDescription = "Limpiar",
								tint = VideoMaxTheme.extended.textTertiary
							)
						}
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color(
						MaterialTheme.colorScheme.primary.red,
						MaterialTheme.colorScheme.primary.green,
						MaterialTheme.colorScheme.primary.blue,
						0.10f
					).compositeOver(MaterialTheme.colorScheme.surface)
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
			when (viewModel.type) {
				Screen.SmartCollection.FAVORITES -> {
					val favorites by viewModel.favorites.collectAsStateWithLifecycle()
					VideoCollectionGrid(
						videos = favorites,
						onOpen = { id ->
							viewModel.prepareFromVideos(favorites, id)
							onOpenPlayer(id)
						},
						onFavorite = viewModel::toggleFavorite
					)
				}
				Screen.SmartCollection.MOST_PLAYED -> {
					val videos by viewModel.mostPlayed.collectAsStateWithLifecycle()
					VideoCollectionGrid(
						videos = videos,
						onOpen = { id ->
							viewModel.prepareFromVideos(videos, id)
							onOpenPlayer(id)
						},
						onFavorite = viewModel::toggleFavorite
					)
				}
				Screen.SmartCollection.HISTORY, Screen.SmartCollection.RECENT -> {
					val history by viewModel.history.collectAsStateWithLifecycle()
					if (history.isEmpty()) {
						EmptyState(
							title = "Sin historial",
							subtitle = "Los videos que veas aparecerán acá."
						)
					} else {
						LazyColumn {
							items(history, key = { it.videoId }) { item ->
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.clickable {
											viewModel.prepareFromHistory(history, item.videoId)
											onOpenPlayer(item.videoId)
										}
										.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
									verticalAlignment = Alignment.CenterVertically
								) {
									VideoThumbnail(
										uri = item.videoUri,
										modifier = Modifier
											.width(120.dp)
											.height(68.dp)
									)
									Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
									Column(modifier = Modifier.weight(1f)) {
										Text(
											item.displayName,
											style = MaterialTheme.typography.titleMedium,
											color = VideoMaxTheme.extended.textPrimary,
											maxLines = 2
										)
										Text(
											"${Formatters.formatDuration(item.positionMs)} / ${Formatters.formatDuration(item.durationMs)}",
											style = MaterialTheme.typography.bodyMedium,
											color = VideoMaxTheme.extended.textTertiary
										)
									}
								}
							}
						}
					}
				}
				Screen.SmartCollection.QUEUE -> {
					val queue by viewModel.queueVideos.collectAsStateWithLifecycle()
					VideoCollectionGrid(
						videos = queue,
						onOpen = { id ->
							viewModel.prepareFromVideos(queue, id)
							onOpenPlayer(id)
						},
						onFavorite = viewModel::toggleFavorite
					)
				}
				else -> EmptyState("Desconocido", "Colección no soportada")
			}
		}
	}
}

@Composable
private fun VideoCollectionGrid(
	videos: List<Video>,
	modifier: Modifier = Modifier,
	onOpen: (Long) -> Unit,
	onFavorite: (Long) -> Unit
) {
	if (videos.isEmpty()) {
		EmptyState(
			title = "Vacío por acá",
			subtitle = "Agregá favoritos o reproducí más videos.",
			modifier = modifier
		)
	} else {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(168.dp),
			contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
			verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
			horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
			modifier = modifier.fillMaxSize()
		) {
			items(videos, key = { it.id }) { video ->
				VideoGridItem(
					video = video,
					onClick = { onOpen(video.id) },
					onFavoriteClick = { onFavorite(video.id) }
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
	onBack: () -> Unit,
	onOpenPlayer: (Long) -> Unit,
	viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
	val playlist by viewModel.playlist.collectAsStateWithLifecycle()

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text(playlist?.playlist?.name ?: "Playlist", color = VideoMaxTheme.extended.textPrimary) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = VideoMaxTheme.extended.textPrimary)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color(
						MaterialTheme.colorScheme.primary.red,
						MaterialTheme.colorScheme.primary.green,
						MaterialTheme.colorScheme.primary.blue,
						0.10f
					).compositeOver(MaterialTheme.colorScheme.surface)
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
			val videos = playlist?.videos.orEmpty()
			if (videos.isEmpty()) {
				EmptyState(
					title = "Lista vacía",
					subtitle = "Agregá videos desde la biblioteca."
				)
			} else {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(168.dp),
					contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
					verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
					horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
					modifier = Modifier.fillMaxSize()
				) {
					items(videos, key = { it.id }) { video ->
						VideoGridItem(
							video = video,
							onClick = {
								viewModel.preparePlayback(video.id)
								onOpenPlayer(video.id)
							},
							onFavoriteClick = {}
						)
					}
				}
			}
		}
	}
}
