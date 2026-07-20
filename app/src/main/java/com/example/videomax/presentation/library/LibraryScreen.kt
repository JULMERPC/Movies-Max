package com.example.videomax.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videomax.domain.model.SortOption
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoGridItem
import com.example.videomax.presentation.components.VideoListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
	onOpenPlayer: (Long) -> Unit,
	onOpenDetails: (Long) -> Unit,
	viewModel: LibraryViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }
	var sortMenuExpanded by remember { mutableStateOf(false) }

	LaunchedEffect(state.message) {
		state.message?.let {
			snackbarHostState.showSnackbar(it)
			viewModel.clearMessage()
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Column {
						Text("videomax")
						Text(
							text = "${state.videos.size} videos",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				},
				actions = {
					IconButton(onClick = viewModel::refresh) {
						Icon(Icons.Default.Refresh, contentDescription = "Scan")
					}
					IconButton(onClick = viewModel::toggleLayout) {
						Icon(
							if (state.isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
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
										viewModel.onSortSelected(option)
										sortMenuExpanded = false
									}
								)
							}
						}
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.background
				)
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			TextField(
				value = state.query,
				onValueChange = viewModel::onQueryChange,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp),
				placeholder = { Text("Search videos or folders") },
				leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
				singleLine = true,
				shape = MaterialTheme.shapes.large,
				colors = TextFieldDefaults.colors(
					focusedIndicatorColor = Color.Transparent,
					unfocusedIndicatorColor = Color.Transparent,
					disabledIndicatorColor = Color.Transparent
				)
			)

			if (state.folders.isNotEmpty()) {
				LazyRow(
					contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					item {
						FilterChip(
							selected = state.selectedFolder == null,
							onClick = { viewModel.selectFolder(null) },
							label = { Text("All") }
						)
					}
					items(state.folders, key = { it }) { folder ->
						FilterChip(
							selected = state.selectedFolder == folder,
							onClick = { viewModel.selectFolder(folder) },
							label = { Text(folder) }
						)
					}
				}
			}

			when {
				state.isScanning && state.videos.isEmpty() -> {
					Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator()
					}
				}
				state.videos.isEmpty() -> {
					EmptyState(
						title = "No videos found",
						subtitle = "Grant media permission and tap refresh to scan your device."
					)
				}
				state.isGrid -> {
					LazyVerticalGrid(
						columns = GridCells.Adaptive(168.dp),
						contentPadding = PaddingValues(16.dp),
						verticalArrangement = Arrangement.spacedBy(12.dp),
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.fillMaxSize()
					) {
						items(state.videos, key = { it.id }) { video ->
							VideoGridItem(
								video = video,
								onClick = {
									viewModel.preparePlayback(video.id)
									onOpenPlayer(video.id)
								},
								onFavoriteClick = { viewModel.onFavorite(video.id) }
							)
						}
					}
				}
				else -> {
					LazyColumn(modifier = Modifier.fillMaxSize()) {
						items(state.videos, key = { it.id }) { video ->
							VideoListItem(
								video = video,
								onClick = {
									viewModel.preparePlayback(video.id)
									onOpenPlayer(video.id)
								},
								onFavoriteClick = { viewModel.onFavorite(video.id) }
							)
						}
					}
				}
			}
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
