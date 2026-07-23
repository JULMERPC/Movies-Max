package com.example.videomax.presentation.playlists

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.PlaybackHistory
import com.example.videomax.domain.model.Playlist
import com.example.videomax.domain.model.PlaylistWithVideos
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.repository.HistoryRepository
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.repository.VideoRepository
import com.example.videomax.domain.usecase.CreatePlaylistUseCase
import com.example.videomax.domain.usecase.GetVideoByIdUseCase
import com.example.videomax.domain.usecase.ObserveFavoritesUseCase
import com.example.videomax.domain.usecase.ObserveHistoryUseCase
import com.example.videomax.domain.usecase.ObservePlaylistsUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoGridItem
import com.example.videomax.presentation.components.VideoThumbnail
import com.example.videomax.presentation.navigation.Screen
import com.example.videomax.presentation.player.PlaybackQueue
import com.example.videomax.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartCategory(
	val type: String,
	val title: String,
	val subtitle: String,
	val icon: ImageVector
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
	observePlaylists: ObservePlaylistsUseCase,
	private val createPlaylist: CreatePlaylistUseCase,
	private val playlistRepository: PlaylistRepository,
	observeFavorites: ObserveFavoritesUseCase,
	observeHistory: ObserveHistoryUseCase,
	videoRepository: VideoRepository
) : ViewModel() {
	val playlists: StateFlow<List<Playlist>> = observePlaylists()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val favoritesCount: StateFlow<Int> = observeFavorites()
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	val historyCount: StateFlow<Int> = observeHistory()
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	val mostPlayedCount: StateFlow<Int> = videoRepository.observeMostPlayed(200)
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

@HiltViewModel
class SmartCollectionViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	observeFavorites: ObserveFavoritesUseCase,
	observeHistory: ObserveHistoryUseCase,
	videoRepository: VideoRepository,
	private val getVideoById: GetVideoByIdUseCase,
	private val historyRepository: HistoryRepository,
	private val toggleFavorite: ToggleFavoriteUseCase,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	val type: String = checkNotNull(savedStateHandle["type"])

	val favorites: StateFlow<List<Video>> = observeFavorites()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val history: StateFlow<List<PlaybackHistory>> = observeHistory()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val mostPlayed: StateFlow<List<Video>> = videoRepository.observeMostPlayed(100)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	private val _queueVideos = MutableStateFlow<List<Video>>(emptyList())
	val queueVideos: StateFlow<List<Video>> = _queueVideos.asStateFlow()

	init {
		if (type == Screen.SmartCollection.QUEUE) {
			viewModelScope.launch { refreshQueue() }
		}
	}

	private suspend fun refreshQueue() {
		_queueVideos.value = playbackQueue.ids().mapNotNull { getVideoById(it) }
	}

	fun toggleFavorite(videoId: Long) {
		viewModelScope.launch { toggleFavorite(videoId) }
	}

	fun clearHistory() {
		viewModelScope.launch { historyRepository.clearHistory() }
	}

	fun prepareFromVideos(videos: List<Video>, videoId: Long) {
		playbackQueue.setQueue(videos.map { it.id }, videoId)
	}

	fun prepareFromHistory(items: List<PlaybackHistory>, videoId: Long) {
		playbackQueue.setQueue(items.map { it.videoId }, videoId)
	}

	fun prepareFromQueue(videoId: Long) {
		playbackQueue.ensureSingle(videoId)
	}
}

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
		SmartCategory(Screen.SmartCollection.FAVORITES, "Favorites", "$favoritesCount videos", Icons.Default.Favorite),
		SmartCategory(Screen.SmartCollection.HISTORY, "History", "$historyCount watched", Icons.Default.History),
		SmartCategory(Screen.SmartCollection.MOST_PLAYED, "Most played", "$mostPlayedCount videos", Icons.Default.Whatshot),
		SmartCategory(Screen.SmartCollection.RECENT, "Recently played", "Continue watching", Icons.Default.History),
		SmartCategory(Screen.SmartCollection.QUEUE, "Playback queue", "Current session", Icons.AutoMirrored.Filled.QueueMusic)
	)

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
				title = { Text("Playlists") },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
					titleContentColor = MaterialTheme.colorScheme.onSurface
				)
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = { showDialog = true }) {
				Icon(Icons.Default.Add, contentDescription = "Create playlist")
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
				contentPadding = PaddingValues(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				item {
					Text(
						text = "Smart collections",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.primary
					)
				}
				items(smartCategories, key = { it.type }) { category ->
					SmartCategoryCard(category = category, onClick = { onOpenSmart(category.type) })
				}
				item {
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "Your playlists",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.primary
					)
				}
				if (playlists.isEmpty()) {
					item {
						Text(
							text = "Create a playlist to organize your videos.",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				} else {
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

@Composable
private fun SmartCategoryCard(category: SmartCategory, onClick: () -> Unit) {
	Card(
		onClick = onClick,
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				imageVector = category.icon,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(28.dp)
			)
			Spacer(modifier = Modifier.width(14.dp))
			Column(modifier = Modifier.weight(1f)) {
				Text(category.title, style = MaterialTheme.typography.titleMedium)
				Text(
					category.subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
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
		Screen.SmartCollection.FAVORITES -> "Favorites"
		Screen.SmartCollection.HISTORY -> "History"
		Screen.SmartCollection.MOST_PLAYED -> "Most played"
		Screen.SmartCollection.RECENT -> "Recently played"
		Screen.SmartCollection.QUEUE -> "Playback queue"
		else -> "Collection"
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
			TopAppBar(
				title = { Text(title) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				},
				actions = {
					if (viewModel.type == Screen.SmartCollection.HISTORY ||
						viewModel.type == Screen.SmartCollection.RECENT
					) {
						IconButton(onClick = viewModel::clearHistory) {
							Icon(Icons.Default.Delete, contentDescription = "Clear")
						}
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
							title = "No history yet",
							subtitle = "Videos you watch will appear here."
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
										.padding(horizontal = 16.dp, vertical = 10.dp),
									verticalAlignment = Alignment.CenterVertically
								) {
									VideoThumbnail(
										uri = item.videoUri,
										modifier = Modifier
											.width(120.dp)
											.height(68.dp)
									)
									Spacer(modifier = Modifier.width(12.dp))
									Column(modifier = Modifier.weight(1f)) {
										Text(item.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2)
										Text(
											"${Formatters.formatDuration(item.positionMs)} / ${Formatters.formatDuration(item.durationMs)}",
											style = MaterialTheme.typography.bodyMedium,
											color = MaterialTheme.colorScheme.onSurfaceVariant
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
				else -> EmptyState("Unknown", "Unsupported collection")
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
			title = "Nothing here yet",
			subtitle = "Add favorites or play more videos.",
			modifier = modifier
		)
	} else {
		LazyVerticalGrid(
			columns = GridCells.Adaptive(168.dp),
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
			horizontalArrangement = Arrangement.spacedBy(14.dp),
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
				title = { Text(playlist?.playlist?.name ?: "Playlist") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
			val videos = playlist?.videos.orEmpty()
			if (videos.isEmpty()) {
				EmptyState(
					title = "Empty playlist",
					subtitle = "Add videos from the library."
				)
			} else {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(168.dp),
					contentPadding = PaddingValues(16.dp),
					verticalArrangement = Arrangement.spacedBy(14.dp),
					horizontalArrangement = Arrangement.spacedBy(14.dp),
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
