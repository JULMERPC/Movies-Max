package com.example.videomax.presentation.library

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.videomax.domain.model.SortOption
import com.example.videomax.domain.model.Video
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoGridItem
import com.example.videomax.presentation.components.VideoListItem
import com.example.videomax.presentation.components.VideoMenuAction
import com.example.videomax.presentation.theme.VideoMaxDimens
import com.example.videomax.presentation.theme.VideoMaxTheme
import com.example.videomax.presentation.theme.screenGradient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
	onOpenPlayer: (Long) -> Unit,
	onOpenDetails: (Long) -> Unit,
	viewModel: LibraryViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val pagingItems = viewModel.videos.collectAsLazyPagingItems()
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val playlists by viewModel.playlists.collectAsStateWithLifecycle()

	var renameVideoId by remember { mutableStateOf<Long?>(null) }
	var renameText by remember { mutableStateOf("") }
	var deleteVideoId by remember { mutableStateOf<Long?>(null) }
	var deleteVideoName by remember { mutableStateOf("") }
	var playlistTargetVideoId by remember { mutableStateOf<Long?>(null) }

	LaunchedEffect(state.message) {
		state.message?.let {
			snackbarHostState.showSnackbar(it)
			viewModel.clearMessage()
		}
	}

	val onMenuAction: (VideoMenuAction) -> Unit = { action ->
		when (action) {
			is VideoMenuAction.MoveToPrivate -> viewModel.moveToPrivate(action.videoId)
			is VideoMenuAction.PlayBackground -> {
				viewModel.markVideoSeen(action.videoId)
				scope.launch {
					val video = viewModel.getVideoById(action.videoId)
					if (video != null) {
						val serviceIntent = Intent(context, com.example.videomax.service.BackgroundAudioService::class.java).apply {
							putExtra(com.example.videomax.service.BackgroundAudioService.EXTRA_VIDEO_URI, video.uri)
							putExtra(com.example.videomax.service.BackgroundAudioService.EXTRA_VIDEO_NAME, video.displayName)
						}
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							context.startForegroundService(serviceIntent)
						} else {
							context.startService(serviceIntent)
						}
					}
				}
			}
			is VideoMenuAction.AddToPlaylist -> {
				playlistTargetVideoId = action.videoId
			}
			is VideoMenuAction.Rename -> {
				renameVideoId = action.videoId
				renameText = ""
			}
			is VideoMenuAction.Share -> {
				scope.launch {
					val video = viewModel.getVideoById(action.videoId)
					if (video != null) {
						val shareIntent = Intent(Intent.ACTION_SEND).apply {
							type = "video/*"
							putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(video.uri))
							addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
						}
						context.startActivity(Intent.createChooser(shareIntent, "Compartir video"))
					}
				}
			}
			is VideoMenuAction.Delete -> {
				deleteVideoId = action.videoId
				deleteVideoName = ""
			}
		}
	}

	val gradient = screenGradient()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			LibraryTopBar(
				videoCountProvider = { state.videoCount },
				isGridProvider = { state.isGrid },
				isSearchOpenProvider = { state.isSearchOpen },
				queryProvider = { state.query },
				isScanningProvider = { state.isScanning },
				onRefresh = viewModel::refresh,
				onToggleLayout = viewModel::toggleLayout,
				onSortSelected = viewModel::onSortSelected,
				onToggleSearch = viewModel::toggleSearch,
				onQueryChange = viewModel::onQueryChange
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(gradient)
				.padding(padding)
		) {
			Column(
				modifier = Modifier.fillMaxSize()
			) {
				ScanningProgressBar(
					isScanningProvider = { state.isScanning },
					progressProvider = { state.scanProgress }
				)

				FilterModeSelector(
					selectedMode = state.filterMode,
					onModeSelected = viewModel::setFilterMode
				)

				when (state.filterMode) {
					LibraryFilterMode.ALL_VIDEOS -> {
					VideoContent(
						pagingItems = pagingItems,
						isGridProvider = { state.isGrid },
						isScanningProvider = { state.isScanning },
						onVideoClick = { videoId ->
							scope.launch {
								viewModel.markVideoSeen(videoId)
								viewModel.preparePlayback(videoId)
								onOpenPlayer(videoId)
							}
						},
						onFavoriteClick = viewModel::onFavorite,
						onMenuAction = onMenuAction
					)
					}
					LibraryFilterMode.ALL_FOLDERS -> {
						FolderGrid(
							folders = state.folders,
							onFolderClick = { folder ->
								viewModel.selectFolder(folder)
								viewModel.setFilterMode(LibraryFilterMode.ALL_VIDEOS)
							}
						)
					}
					LibraryFilterMode.FOLDER_TREE -> {
						FolderTree(
							folders = state.folders,
							onFolderClick = { folder ->
								viewModel.selectFolder(folder)
								viewModel.setFilterMode(LibraryFilterMode.ALL_VIDEOS)
							}
						)
					}
				}
			}
		}
	}

	if (renameVideoId != null) {
		LaunchedEffect(renameVideoId) {
			if (renameText.isEmpty()) {
				val video = viewModel.getVideoById(renameVideoId!!)
				renameText = video?.displayName ?: ""
			}
		}
		AlertDialog(
			onDismissRequest = { renameVideoId = null },
			title = { Text("Renombrar") },
			text = {
				OutlinedTextField(
					value = renameText,
					onValueChange = { renameText = it },
					label = { Text("Nombre") },
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)
			},
			confirmButton = {
				TextButton(onClick = {
					if (renameText.isNotBlank()) {
						viewModel.renameVideo(renameVideoId!!, renameText.trim())
					}
					renameVideoId = null
				}) { Text("Guardar") }
			},
			dismissButton = {
				TextButton(onClick = { renameVideoId = null }) { Text("Cancelar") }
			},
			containerColor = MaterialTheme.colorScheme.surface
		)
	}

	if (deleteVideoId != null) {
		LaunchedEffect(deleteVideoId) {
			if (deleteVideoName.isEmpty()) {
				val video = viewModel.getVideoById(deleteVideoId!!)
				deleteVideoName = video?.displayName ?: ""
			}
		}
		AlertDialog(
			onDismissRequest = { deleteVideoId = null },
			title = { Text("Eliminar video") },
			text = { Text("¿Eliminar \"${deleteVideoName}\"? Esta acción no se puede deshacer.") },
			confirmButton = {
				TextButton(onClick = {
					viewModel.deleteVideo(deleteVideoId!!)
					deleteVideoId = null
				}) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
			},
			dismissButton = {
				TextButton(onClick = { deleteVideoId = null }) { Text("Cancelar") }
			},
			containerColor = MaterialTheme.colorScheme.surface
		)
	}

	if (playlistTargetVideoId != null) {
		AlertDialog(
			onDismissRequest = { playlistTargetVideoId = null },
			title = { Text("Añadir a lista") },
			text = {
				if (playlists.isEmpty()) {
					Text("No hay listas. Creá una desde la pestaña Playlists.")
				} else {
					Column {
						playlists.forEach { playlist ->
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.clickable {
										viewModel.addToPlaylist(playlist.id, playlistTargetVideoId!!)
										playlistTargetVideoId = null
									}
									.padding(vertical = 12.dp, horizontal = 4.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									imageVector = Icons.Default.Folder,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.primary,
									modifier = Modifier.size(24.dp)
								)
								Spacer(modifier = Modifier.width(12.dp))
								Text(
									text = playlist.name,
									style = MaterialTheme.typography.bodyLarge
								)
							}
						}
					}
				}
			},
			confirmButton = {
				TextButton(onClick = { playlistTargetVideoId = null }) { Text("Cancelar") }
			},
			containerColor = MaterialTheme.colorScheme.surface
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
	videoCountProvider: () -> Int,
	isGridProvider: () -> Boolean,
	isSearchOpenProvider: () -> Boolean,
	queryProvider: () -> String,
	isScanningProvider: () -> Boolean,
	onRefresh: () -> Unit,
	onToggleLayout: () -> Unit,
	onSortSelected: (SortOption) -> Unit,
	onToggleSearch: () -> Unit,
	onQueryChange: (String) -> Unit
) {
	var sortMenuExpanded by remember { mutableStateOf(false) }
	val videoCount = videoCountProvider()
	val isGrid = isGridProvider()
	val isSearchOpen = isSearchOpenProvider()
	val query = queryProvider()
	val isScanning = isScanningProvider()

	Column {
		TopAppBar(
			title = {
				Column {
					Text("videomax", style = MaterialTheme.typography.headlineSmall, color = VideoMaxTheme.extended.textPrimary)
					Text(
						text = "$videoCount videos",
						style = MaterialTheme.typography.bodyMedium,
						color = VideoMaxTheme.extended.textTertiary
					)
				}
			},
		actions = {
			IconButton(onClick = onRefresh, enabled = !isScanning) {
				Icon(
					Icons.Default.Refresh,
					contentDescription = "Escanear",
					tint = VideoMaxTheme.extended.textSecondary
				)
			}
			IconButton(onClick = onToggleLayout) {
					Icon(
						if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
						contentDescription = "Cambiar vista",
						tint = VideoMaxTheme.extended.textSecondary
					)
				}
				IconButton(onClick = onToggleSearch) {
					Icon(
						if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
						contentDescription = "Search",
						tint = VideoMaxTheme.extended.textSecondary
					)
				}
				Box {
					IconButton(onClick = { sortMenuExpanded = true }) {
						Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = VideoMaxTheme.extended.textSecondary)
					}
					DropdownMenu(
						expanded = sortMenuExpanded,
						onDismissRequest = { sortMenuExpanded = false },
						shape = RoundedCornerShape(16.dp),
						containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
					) {
						SortOption.entries.forEach { option ->
							DropdownMenuItem(
								text = { Text(option.label()) },
								onClick = {
									onSortSelected(option)
									sortMenuExpanded = false
								}
							)
						}
					}
				}
			},
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = Color(
					MaterialTheme.colorScheme.primary.red,
					MaterialTheme.colorScheme.primary.green,
					MaterialTheme.colorScheme.primary.blue,
					0.10f
				).compositeOver(Color.White),
				titleContentColor = MaterialTheme.colorScheme.onSurface
			)
		)

		AnimatedVisibility(
			visible = isSearchOpen,
			enter = expandVertically() + fadeIn(),
			exit = shrinkVertically() + fadeOut()
		) {
			TextField(
				value = query,
				onValueChange = onQueryChange,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingXs),
				placeholder = { Text("Buscar videos o carpetas") },
				leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
				singleLine = true,
				shape = MaterialTheme.shapes.extraLarge,
				colors = TextFieldDefaults.colors(
					focusedIndicatorColor = Color.Transparent,
					unfocusedIndicatorColor = Color.Transparent,
					disabledIndicatorColor = Color.Transparent
				)
			)
		}
	}
}

@Composable
fun FilterModeSelector(
	selectedMode: LibraryFilterMode,
	onModeSelected: (LibraryFilterMode) -> Unit
) {
	LazyRow(
		contentPadding = PaddingValues(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
		horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm)
	) {
		item {
			FilterChip(
				selected = selectedMode == LibraryFilterMode.ALL_VIDEOS,
				onClick = { onModeSelected(LibraryFilterMode.ALL_VIDEOS) },
				label = { Text("Todos los videos") },
				leadingIcon = {
					Icon(
						Icons.Default.VideoFile,
						contentDescription = null,
						modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
					)
				}
			)
		}
		item {
			FilterChip(
				selected = selectedMode == LibraryFilterMode.ALL_FOLDERS,
				onClick = { onModeSelected(LibraryFilterMode.ALL_FOLDERS) },
				label = { Text("Todas las carpetas") },
				leadingIcon = {
					Icon(
						Icons.Default.Folder,
						contentDescription = null,
						modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
					)
				}
			)
		}
		item {
			FilterChip(
				selected = selectedMode == LibraryFilterMode.FOLDER_TREE,
				onClick = { onModeSelected(LibraryFilterMode.FOLDER_TREE) },
				label = { Text("Árbol de carpetas") },
				leadingIcon = {
					Icon(
						Icons.Default.CreateNewFolder,
						contentDescription = null,
						modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
					)
				}
			)
		}
	}
}

@Composable
fun FolderGrid(
	folders: List<String>,
	onFolderClick: (String) -> Unit
) {
	if (folders.isEmpty()) {
		EmptyState(
			title = "No hay carpetas",
			subtitle = "Escanea la biblioteca para detectar carpetas."
		)
		return
	}
	LazyVerticalGrid(
		columns = GridCells.Adaptive(168.dp),
		contentPadding = PaddingValues(VideoMaxDimens.spacingMd),
		verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm),
		horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingSm),
		modifier = Modifier.fillMaxSize()
	) {
		items(folders.size, key = { folders[it] }) { index ->
			val folder = folders[index]
			val folderName = folder.substringAfterLast('/', folder)
			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1.4f)
					.clickable { onFolderClick(folder) },
				shape = MaterialTheme.shapes.medium,
				color = MaterialTheme.colorScheme.surfaceContainerHigh
			) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(VideoMaxDimens.spacingMd),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Icon(
						Icons.Default.Folder,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(VideoMaxDimens.iconSizeXl)
					)
					Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
					Text(
						text = folderName,
						style = MaterialTheme.typography.bodyMedium,
						color = VideoMaxTheme.extended.textPrimary,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis
					)
				}
			}
		}
	}
}

@Composable
fun FolderTree(
	folders: List<String>,
	onFolderClick: (String) -> Unit
) {
	if (folders.isEmpty()) {
		EmptyState(
			title = "No hay carpetas",
			subtitle = "Escanea la biblioteca para detectar carpetas."
		)
		return
	}

	val tree = remember(folders) { buildFolderTree(folders) }

	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(VideoMaxDimens.spacingMd),
		verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXxs)
	) {
		items(tree.size, key = { tree[it].path }) { index ->
			val node = tree[index]
			FolderTreeNode(
				node = node,
				onClick = { onFolderClick(node.path) }
			)
		}
	}
}

@Composable
private fun FolderTreeNode(
	node: FolderTreeNodeData,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(
				start = (VideoMaxDimens.spacingLg.value.toInt() + node.depth * 24).dp,
				top = VideoMaxDimens.spacingSm,
				bottom = VideoMaxDimens.spacingSm,
				end = VideoMaxDimens.spacingLg
			),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			if (node.hasChildren) Icons.Default.FolderOpen else Icons.Default.Folder,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary.copy(alpha = VideoMaxDimens.alphaHigh),
			modifier = Modifier.size(VideoMaxDimens.iconSizeSm)
		)
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
		Text(
			text = node.name,
			style = MaterialTheme.typography.bodyMedium,
			color = VideoMaxTheme.extended.textPrimary,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

data class FolderTreeNodeData(
	val name: String,
	val path: String,
	val depth: Int,
	val hasChildren: Boolean
)

private fun buildFolderTree(folders: List<String>): List<FolderTreeNodeData> {
	val sorted = folders.sorted()
	val pathSet = sorted.toSet()
	val result = mutableListOf<FolderTreeNodeData>()

	for (folder in sorted) {
		val segments = folder.split('/').filter { it.isNotEmpty() }
		for (i in segments.indices) {
			val subPath = "/" + segments.take(i + 1).joinToString("/")
			if (subPath !in pathSet) continue
			val depth = i
			val name = segments[i]
			val hasChildren = sorted.any { other ->
				other != folder && other.startsWith("$subPath/") && other.length > subPath.length + 1
			}
			if (result.none { it.path == subPath }) {
				result.add(
					FolderTreeNodeData(
						name = name,
						path = subPath,
						depth = depth,
						hasChildren = hasChildren
					)
				)
			}
		}
	}
	return result
}

@Composable
fun ScanningProgressBar(
	isScanningProvider: () -> Boolean,
	progressProvider: () -> Float
) {
	val isScanning = isScanningProvider()
	AnimatedVisibility(
		visible = isScanning,
		enter = fadeIn(),
		exit = fadeOut()
	) {
		val progress = progressProvider()
		LinearProgressIndicator(
			progress = { progress.coerceIn(0f, 1f) },
			modifier = Modifier.fillMaxWidth()
		)
	}
}

@Composable
fun VideoContent(
	pagingItems: androidx.paging.compose.LazyPagingItems<Video>,
	isGridProvider: () -> Boolean,
	isScanningProvider: () -> Boolean,
	onVideoClick: (Long) -> Unit,
	onFavoriteClick: (Long) -> Unit,
	onMenuAction: ((com.example.videomax.presentation.components.VideoMenuAction) -> Unit)? = null
) {
	val isGrid = isGridProvider()
	val isScanning = isScanningProvider()

	when {
		pagingItems.itemCount == 0 && isScanning -> {
			LibraryLoadingPlaceholders(isGrid = isGrid)
		}
		pagingItems.itemCount == 0 && !isScanning -> {
			EmptyState(
				title = "No se encontraron videos",
				subtitle = "Concede permisos de medios y toca actualizar para escanear."
			)
		}
		else -> {
			if (isGrid) {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(168.dp),
					contentPadding = PaddingValues(VideoMaxDimens.spacingMd),
					verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
					horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
					modifier = Modifier.fillMaxSize()
				) {
					items(
						count = pagingItems.itemCount,
						key = pagingItems.itemKey { it.id }
					) { index ->
						val video = pagingItems[index]
						if (video == null) {
							VideoGridSkeleton()
						} else {
						VideoGridItem(
							video = video,
							onClick = { onVideoClick(video.id) },
							onFavoriteClick = { onFavoriteClick(video.id) },
							onMenuAction = onMenuAction,
							showOnlyThumbnail = false
						)
						}
					}
				}
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(vertical = VideoMaxDimens.spacingSm)
				) {
					items(
						count = pagingItems.itemCount,
						key = pagingItems.itemKey { it.id }
					) { index ->
						val video = pagingItems[index]
						if (video == null) {
							VideoListSkeleton()
						} else {
						VideoListItem(
							video = video,
							onClick = { onVideoClick(video.id) },
							onFavoriteClick = { onFavoriteClick(video.id) },
							onMenuAction = onMenuAction
						)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun LibraryLoadingPlaceholders(isGrid: Boolean) {
	if (isGrid) {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(168.dp),
			contentPadding = PaddingValues(VideoMaxDimens.spacingMd),
			verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
			horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
			modifier = Modifier.fillMaxSize(),
			userScrollEnabled = false
		) {
			items(12) { VideoGridSkeleton() }
		}
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(vertical = VideoMaxDimens.spacingSm),
			userScrollEnabled = false
		) {
			items(10) { VideoListSkeleton() }
		}
	}
}

@Composable
private fun VideoGridSkeleton() {
	Column(modifier = Modifier.fillMaxWidth()) {
		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f),
			shape = MaterialTheme.shapes.medium,
			color = MaterialTheme.colorScheme.surfaceContainerHigh
		) {}
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
		Surface(
			modifier = Modifier
				.fillMaxWidth(0.9f)
				.height(14.dp),
			shape = MaterialTheme.shapes.small,
			color = MaterialTheme.colorScheme.surfaceContainerHigh
		) {}
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXs))
		Surface(
			modifier = Modifier
				.fillMaxWidth(0.55f)
				.height(12.dp),
			shape = MaterialTheme.shapes.small,
			color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = VideoMaxDimens.alphaDisabled)
		) {}
	}
}

@Composable
private fun VideoListSkeleton() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm)
	) {
		Surface(
			modifier = Modifier
				.width(120.dp)
				.aspectRatio(16f / 9f),
			shape = MaterialTheme.shapes.medium,
			color = MaterialTheme.colorScheme.surfaceContainerHigh
		) {}
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
		Column(modifier = Modifier.weight(1f)) {
			Surface(
				modifier = Modifier
					.fillMaxWidth(0.85f)
					.height(16.dp),
				shape = MaterialTheme.shapes.small,
				color = MaterialTheme.colorScheme.surfaceContainerHigh
			) {}
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
			Surface(
				modifier = Modifier
					.fillMaxWidth(0.45f)
					.height(12.dp),
				shape = MaterialTheme.shapes.small,
				color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = VideoMaxDimens.alphaDisabled)
			) {}
		}
	}
}

private fun SortOption.label(): String = when (this) {
	SortOption.DATE_DESC -> "Más recientes"
	SortOption.DATE_ASC -> "Más antiguos"
	SortOption.NAME_ASC -> "Nombre A–Z"
	SortOption.NAME_DESC -> "Nombre Z–A"
	SortOption.DURATION_DESC -> "Más largos"
	SortOption.DURATION_ASC -> "Más cortos"
	SortOption.SIZE_DESC -> "Más pesados"
	SortOption.SIZE_ASC -> "Más livianos"
}
