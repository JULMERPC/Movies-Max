package com.example.videomax.presentation.library

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

	LaunchedEffect(state.message) {
		state.message?.let {
			snackbarHostState.showSnackbar(it)
			viewModel.clearMessage()
		}
	}

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
			LibraryTopBar(
				videoCountProvider = { state.videoCount },
				isGridProvider = { state.isGrid },
				isSearchOpenProvider = { state.isSearchOpen },
				queryProvider = { state.query },
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
									viewModel.preparePlayback(videoId)
									onOpenPlayer(videoId)
								}
							},
							onFavoriteClick = viewModel::onFavorite
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
	videoCountProvider: () -> Int,
	isGridProvider: () -> Boolean,
	isSearchOpenProvider: () -> Boolean,
	queryProvider: () -> String,
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

	Column {
		TopAppBar(
			title = {
				Column {
					Text("videomax", style = MaterialTheme.typography.headlineSmall)
					Text(
						text = "$videoCount videos",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			},
			actions = {
				IconButton(onClick = onRefresh) {
					Icon(Icons.Default.Refresh, contentDescription = "Scan")
				}
				IconButton(onClick = onToggleLayout) {
					Icon(
						if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
						contentDescription = "Toggle layout"
					)
				}
				IconButton(onClick = onToggleSearch) {
					Icon(
						if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
						contentDescription = "Search"
					)
				}
				Box {
					IconButton(onClick = { sortMenuExpanded = true }) {
						Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
					}
					DropdownMenu(
						expanded = sortMenuExpanded,
						onDismissRequest = { sortMenuExpanded = false }
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
				containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
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
					.padding(horizontal = 16.dp, vertical = 4.dp),
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
		contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
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
						modifier = Modifier.size(18.dp)
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
						modifier = Modifier.size(18.dp)
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
						modifier = Modifier.size(18.dp)
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
		contentPadding = PaddingValues(12.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		modifier = Modifier.fillMaxSize()
	) {
		items(folders.size, key = { folders[it] }) { index ->
			val folder = folders[index]
			val folderName = folder.substringAfterLast('/', folder)
			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.clickable { onFolderClick(folder) },
				shape = MaterialTheme.shapes.medium,
				color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
			) {
				Row(
					modifier = Modifier.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						Icons.Default.Folder,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(32.dp)
					)
					Spacer(modifier = Modifier.width(12.dp))
					Text(
						text = folderName,
						style = MaterialTheme.typography.bodyLarge,
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
		contentPadding = PaddingValues(12.dp),
		verticalArrangement = Arrangement.spacedBy(2.dp)
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
				start = (16 + node.depth * 24).dp,
				top = 6.dp,
				bottom = 6.dp,
				end = 16.dp
			),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			if (node.hasChildren) Icons.Default.FolderOpen else Icons.Default.Folder,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
			modifier = Modifier.size(20.dp)
		)
		Spacer(modifier = Modifier.width(8.dp))
		Text(
			text = node.name,
			style = MaterialTheme.typography.bodyMedium,
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
	onFavoriteClick: (Long) -> Unit
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
					contentPadding = PaddingValues(12.dp),
					verticalArrangement = Arrangement.spacedBy(14.dp),
					horizontalArrangement = Arrangement.spacedBy(12.dp),
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
								showOnlyThumbnail = false
							)
						}
					}
				}
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(vertical = 8.dp)
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
								onFavoriteClick = { onFavoriteClick(video.id) }
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
			contentPadding = PaddingValues(12.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier.fillMaxSize(),
			userScrollEnabled = false
		) {
			items(12) { VideoGridSkeleton() }
		}
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(vertical = 8.dp),
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
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
		) {}
		Spacer(modifier = Modifier.height(8.dp))
		Surface(
			modifier = Modifier
				.fillMaxWidth(0.9f)
				.height(14.dp),
			shape = MaterialTheme.shapes.small,
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
		) {}
		Spacer(modifier = Modifier.height(6.dp))
		Surface(
			modifier = Modifier
				.fillMaxWidth(0.55f)
				.height(12.dp),
			shape = MaterialTheme.shapes.small,
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
		) {}
	}
}

@Composable
private fun VideoListSkeleton() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp)
	) {
		Surface(
			modifier = Modifier
				.width(120.dp)
				.aspectRatio(16f / 9f),
			shape = MaterialTheme.shapes.medium,
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
		) {}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Surface(
				modifier = Modifier
					.fillMaxWidth(0.85f)
					.height(16.dp),
				shape = MaterialTheme.shapes.small,
				color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
			) {}
			Spacer(modifier = Modifier.height(8.dp))
			Surface(
				modifier = Modifier
					.fillMaxWidth(0.45f)
					.height(12.dp),
				shape = MaterialTheme.shapes.small,
				color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
