package com.puma.videomax.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.puma.videomax.domain.model.Song
import com.puma.videomax.domain.model.Video
import com.puma.videomax.domain.repository.SongRepository
import com.puma.videomax.domain.usecase.ObserveFavoritesUseCase
import com.puma.videomax.domain.usecase.ToggleFavoriteUseCase
import com.puma.videomax.domain.usecase.ToggleSongFavoriteUseCase
import com.puma.videomax.presentation.components.EmptyState
import com.puma.videomax.presentation.components.VideoGridItem
import com.puma.videomax.presentation.music.SongRow
import com.puma.videomax.presentation.music.TrackActionsSheet
import com.puma.videomax.presentation.player.PlaybackQueue
import com.puma.videomax.service.AudioQueueItem
import com.puma.videomax.service.BackgroundAudioManager
import com.puma.videomax.service.BackgroundAudioService
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoritesTab { VIDEOS, MUSIC }

@HiltViewModel
class FavoritesViewModel @Inject constructor(
	observeFavorites: ObserveFavoritesUseCase,
	private val toggleFavorite: ToggleFavoriteUseCase,
	private val playbackQueue: PlaybackQueue,
	private val songRepository: SongRepository,
	private val toggleSongFavorite: ToggleSongFavoriteUseCase,
) : ViewModel() {
	val favorites: StateFlow<List<Video>> = observeFavorites()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val songFavorites: Flow<PagingData<Song>> = songRepository.pagingFavorites()

	fun toggle(videoId: Long) {
		viewModelScope.launch { runCatching { toggleFavorite(videoId) } }
	}

	fun toggleSong(song: Song) {
		viewModelScope.launch { runCatching { toggleSongFavorite(song.id, !song.isFavorite) } }
	}

	fun preparePlayback(videoId: Long) {
		playbackQueue.setQueue(favorites.value.map { it.id }, videoId)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
	onOpenPlayer: (Long) -> Unit,
	viewModel: FavoritesViewModel = hiltViewModel()
) {
	val favorites by viewModel.favorites.collectAsStateWithLifecycle()
	val lazySongs = viewModel.songFavorites.collectAsLazyPagingItems()
	val context = LocalContext.current
	var tab by remember { mutableStateOf(FavoritesTab.VIDEOS) }
	var trackSheetSong by remember { mutableStateOf<Song?>(null) }

	fun ensureAudioService() {
		val intent = android.content.Intent(context, BackgroundAudioService::class.java)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			context.startForegroundService(intent)
		} else {
			context.startService(intent)
		}
	}

	fun Song.toQueueItem() = AudioQueueItem(
		videoId = id,
		uri = uri,
		displayName = title,
		artist = artist,
		album = album,
		albumId = albumId,
		mimeType = mimeType
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenColor())
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			TopAppBar(
				title = {
					Text(
						"Favoritos",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold,
						color = VideoMaxTheme.extended.textPrimary
					)
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent
				)
			)

			// Música / Video switch at the top.
			TabRow(
				selectedTabIndex = tab.ordinal,
				containerColor = Color.Transparent,
				contentColor = MaterialTheme.colorScheme.primary
			) {
				Tab(
					selected = tab == FavoritesTab.VIDEOS,
					onClick = { tab = FavoritesTab.VIDEOS },
					text = { Text("Videos") }
				)
				Tab(
					selected = tab == FavoritesTab.MUSIC,
					onClick = { tab = FavoritesTab.MUSIC },
					text = { Text("Música") }
				)
			}

			when (tab) {
				FavoritesTab.VIDEOS -> {
					if (favorites.isEmpty()) {
						Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
							EmptyState(
								title = "Sin favoritos",
								subtitle = "Tocá el corazón en cualquier video para fijarlo acá."
							)
						}
					} else {
						LazyVerticalGrid(
							columns = GridCells.Adaptive(168.dp),
							contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
							verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
							horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
							modifier = Modifier.fillMaxSize()
						) {
							// Clave estable por id + contentType homogéneo: el toggle
							// de favorito actualiza solo isFavorite del item vía el
							// Flow de Room, sin reordenar la lista ni mover el scroll.
							items(favorites, key = { it.id }, contentType = { "video" }) { video ->
								VideoGridItem(
									video = video,
									onClick = {
										viewModel.preparePlayback(video.id)
										onOpenPlayer(video.id)
									},
									onFavoriteClick = { viewModel.toggle(video.id) }
								)
							}
						}
					}
				}
				FavoritesTab.MUSIC -> {
					val isPlaying by BackgroundAudioManager.isPlaying.collectAsStateWithLifecycle()
					val favQueue by BackgroundAudioManager.queue.collectAsStateWithLifecycle()
					val favIndex by BackgroundAudioManager.currentIndex.collectAsStateWithLifecycle()
					val currentPlayingId = favQueue.getOrNull(favIndex)?.videoId ?: 0L
					if (lazySongs.itemCount == 0) {
						Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
							EmptyState(
								title = "Sin favoritas",
								subtitle = "Tocá el corazón en cualquier canción para fijarla acá."
							)
						}
					} else {
						LazyColumn(
							modifier = Modifier.fillMaxSize(),
							contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
							verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXs)
						) {
							// Clave estable por id de canción (nunca posicional): al dar
							// favorito el item sale de la lista sin que el resto cambie
							// de clave, así el scroll no salta. itemKey resuelve el peek
							// de forma segura (sin FATAL en bordes de refresh).
							val favSongKey = lazySongs.itemKey { "fav_${it.id}" }
							items(
								count = lazySongs.itemCount,
								key = favSongKey,
								contentType = { "fav_song" }
							) { index ->
								val song = runCatching { lazySongs[index] }.getOrNull() ?: return@items
								SongRow(
									song = song,
									listIndex = index,
									isCurrentlyPlaying = song.id == currentPlayingId,
									isPlaying = isPlaying,
									onClick = {
										val all = runCatching {
											(0 until lazySongs.itemCount).mapNotNull {
												runCatching { lazySongs[it] }.getOrNull()
											}
										}.getOrDefault(emptyList())
										if (all.isNotEmpty()) {
											BackgroundAudioManager.playQueue(
												all.map { it.toQueueItem() },
												index.coerceIn(0, all.lastIndex),
												true
											)
											ensureAudioService()
										}
									},
									onFavorite = { viewModel.toggleSong(song) },
									onPlayNext = { BackgroundAudioManager.playNext(song.toQueueItem()) },
									onMore = { trackSheetSong = song }
								)
							}
						}
					}
				}
			}
		}

		trackSheetSong?.let { sheetSong ->
			TrackActionsSheet(
				song = sheetSong,
				onDismiss = { trackSheetSong = null },
				onPlayNext = {
					BackgroundAudioManager.playNext(
						AudioQueueItem(
							videoId = sheetSong.id,
							uri = sheetSong.uri,
							displayName = sheetSong.title,
							artist = sheetSong.artist,
							album = sheetSong.album,
							albumId = sheetSong.albumId,
							mimeType = sheetSong.mimeType
						)
					)
					trackSheetSong = null
				},
				onAddToQueue = {
					BackgroundAudioManager.addToQueue(
						AudioQueueItem(
							videoId = sheetSong.id,
							uri = sheetSong.uri,
							displayName = sheetSong.title,
							artist = sheetSong.artist,
							album = sheetSong.album,
							albumId = sheetSong.albumId,
							mimeType = sheetSong.mimeType
						)
					)
					trackSheetSong = null
				},
				onToggleFavorite = {
					viewModel.toggleSong(sheetSong)
					trackSheetSong = null
				},
				onShare = {
					val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
						type = "audio/*"
						putExtra(
							android.content.Intent.EXTRA_STREAM,
							runCatching { android.net.Uri.parse(sheetSong.uri) }.getOrNull()
						)
						addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
					}
					context.startActivity(android.content.Intent.createChooser(send, "Compartir"))
					trackSheetSong = null
				}
			)
		}
	}
}
