package com.example.videomax.presentation.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.Playlist
import com.example.videomax.domain.model.PlaylistWithVideos
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.usecase.CreatePlaylistUseCase
import com.example.videomax.domain.usecase.ObservePlaylistsUseCase
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoGridItem
import com.example.videomax.presentation.player.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
	observePlaylists: ObservePlaylistsUseCase,
	private val createPlaylist: CreatePlaylistUseCase,
	private val playlistRepository: PlaylistRepository
) : ViewModel() {
	val playlists: StateFlow<List<Playlist>> = observePlaylists()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun create(name: String) {
		viewModelScope.launch { createPlaylist(name) }
	}

	fun delete(id: Long) {
		viewModelScope.launch { playlistRepository.deletePlaylist(id) }
	}
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	playlistRepository: PlaylistRepository,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

	val playlist: StateFlow<PlaylistWithVideos?> =
		playlistRepository.observePlaylistWithVideos(playlistId)
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	fun preparePlayback(videoId: Long) {
		val ids = playlist.value?.videos?.map { it.id }.orEmpty()
		playbackQueue.setQueue(ids, videoId)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
	onOpenPlaylist: (Long) -> Unit,
	viewModel: PlaylistsViewModel = hiltViewModel()
) {
	val playlists by viewModel.playlists.collectAsStateWithLifecycle()
	var showDialog by remember { mutableStateOf(false) }
	var name by remember { mutableStateOf("") }

	Scaffold(
		topBar = { TopAppBar(title = { Text("Playlists") }) },
		floatingActionButton = {
			FloatingActionButton(onClick = { showDialog = true }) {
				Icon(Icons.Default.Add, contentDescription = "Create playlist")
			}
		}
	) { padding ->
		if (playlists.isEmpty()) {
			EmptyState(
				title = "No playlists",
				subtitle = "Create collections for binge nights, tutorials, or family clips.",
				modifier = Modifier.padding(padding)
			)
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
			) {
				items(playlists, key = { it.id }) { playlist ->
					ListItem(
						headlineContent = { Text(playlist.name) },
						supportingContent = { Text("${playlist.videoCount} videos") },
						leadingContent = {
							Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
						},
						trailingContent = {
							IconButton(onClick = { viewModel.delete(playlist.id) }) {
								Icon(Icons.Default.Delete, contentDescription = "Delete")
							}
						},
						modifier = Modifier
							.fillMaxWidth()
							.clickable { onOpenPlaylist(playlist.id) }
					)
				}
			}
		}
	}

	if (showDialog) {
		AlertDialog(
			onDismissRequest = { showDialog = false },
			title = { Text("New playlist") },
			text = {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text("Name") },
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
				) { Text("Create") }
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) { Text("Cancel") }
			}
		)
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

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(playlist?.playlist?.name ?: "Playlist") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		}
	) { padding ->
		val videos = playlist?.videos.orEmpty()
		if (videos.isEmpty()) {
			EmptyState(
				title = "Empty playlist",
				subtitle = "Add videos from the library long-press menu.",
				modifier = Modifier.padding(padding)
			)
		} else {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(168.dp),
				contentPadding = PaddingValues(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
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
