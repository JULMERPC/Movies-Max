package com.example.videomax.presentation.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

	Scaffold(
		topBar = {
			LibraryTopBar(
				videoCountProvider = { state.videoCount },
				isGridProvider = { state.isGrid },
				onRefresh = viewModel::refresh,
				onToggleLayout = viewModel::toggleLayout,
				onSortSelected = viewModel::onSortSelected
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			ScanningProgressBar(
				isScanningProvider = { state.isScanning },
				progressProvider = { state.scanProgress }
			)

			SearchBar(
				queryProvider = { state.query },
				onQueryChange = viewModel::onQueryChange
			)

			FolderSelector(
				foldersProvider = { state.folders },
				selectedFolderProvider = { state.selectedFolder },
				onFolderSelected = viewModel::selectFolder
			)

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
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
	videoCountProvider: () -> Int,
	isGridProvider: () -> Boolean,
	onRefresh: () -> Unit,
	onToggleLayout: () -> Unit,
	onSortSelected: (SortOption) -> Unit
) {
	var sortMenuExpanded by remember { mutableStateOf(false) }
	val videoCount = videoCountProvider()
	val isGrid = isGridProvider()
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
			containerColor = MaterialTheme.colorScheme.surface
		)
	)
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
fun SearchBar(
	queryProvider: () -> String,
	onQueryChange: (String) -> Unit
) {
	val query = queryProvider()
	TextField(
		value = query,
		onValueChange = onQueryChange,
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		placeholder = { Text("Search videos or folders") },
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

@Composable
fun FolderSelector(
	foldersProvider: () -> List<String>,
	selectedFolderProvider: () -> String?,
	onFolderSelected: (String?) -> Unit
) {
	val folders = foldersProvider()
	val selectedFolder = selectedFolderProvider()
	if (folders.isNotEmpty()) {
		LazyRow(
			contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			item {
				FilterChip(
					selected = selectedFolder == null,
					onClick = { onFolderSelected(null) },
					label = { Text("All") }
				)
			}
			items(folders, key = { it }) { folder ->
				FilterChip(
					selected = selectedFolder == folder,
					onClick = { onFolderSelected(folder) },
					label = { Text(folder) }
				)
			}
		}
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
				title = "No videos found",
				subtitle = "Grant media permission and tap refresh to scan your device."
			)
		}
		else -> {
			if (isGrid) {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(120.dp),
					contentPadding = PaddingValues(8.dp),
					verticalArrangement = Arrangement.spacedBy(6.dp),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
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
								showOnlyThumbnail = true
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

/** Non-blocking placeholders while the first MediaStore batches land in Room. */
@Composable
private fun LibraryLoadingPlaceholders(isGrid: Boolean) {
	if (isGrid) {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(112.dp),
			contentPadding = PaddingValues(12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.fillMaxSize(),
			userScrollEnabled = false
		) {
			items(18) { VideoGridSkeleton() }
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
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(16f / 9f),
		shape = MaterialTheme.shapes.medium,
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
	) {}
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
	SortOption.DATE_DESC -> "Newest first"
	SortOption.DATE_ASC -> "Oldest first"
	SortOption.NAME_ASC -> "Name A–Z"
	SortOption.NAME_DESC -> "Name Z–A"
	SortOption.DURATION_DESC -> "Longest"
	SortOption.DURATION_ASC -> "Shortest"
	SortOption.SIZE_DESC -> "Largest"
	SortOption.SIZE_ASC -> "Smallest"
}
