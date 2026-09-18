package com.puma.videomax.presentation.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.Song
import com.puma.videomax.service.BackgroundAudioManager
import com.puma.videomax.service.AudioQueueItem
import com.puma.videomax.service.BackgroundAudioService
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.puma.videomax.ads.NativeAdSlot
import com.puma.videomax.domain.ads.NativeAdPlacer
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
	viewModel: MusicViewModel = hiltViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()
	val lazySongs = viewModel.songs.collectAsLazyPagingItems()
	val adsEnabled by viewModel.adsEnabled.collectAsState()
	val isPlaying by BackgroundAudioManager.isPlaying.collectAsState()
	val context = LocalContext.current
	var sortMenuExpanded by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()
	// Namida patterns: per-track bottom sheet + queue sheet.
	var trackSheetSong by remember { mutableStateOf<Song?>(null) }
	var showQueueSheet by remember { mutableStateOf(false) }

	fun ensureAudioService() {
		val serviceIntent = Intent(context, BackgroundAudioService::class.java)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(serviceIntent)
		} else {
			context.startService(serviceIntent)
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

	fun startPlayback(songs: List<Song>, index: Int) {
		if (songs.isEmpty()) return
		BackgroundAudioManager.playQueue(
			songs.map { it.toQueueItem() },
			index.coerceIn(0, songs.lastIndex),
			true
		)
		ensureAudioService()
	}

	fun shareSong(song: Song) {
		val uri = runCatching { Uri.parse(song.uri) }.getOrNull() ?: return
		val send = Intent(Intent.ACTION_SEND).apply {
			type = "audio/*"
			putExtra(Intent.EXTRA_STREAM, uri)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
		context.startActivity(Intent.createChooser(send, "Compartir"))
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenColor())
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			TopAppBar(
				title = {
					Text(
						"Música",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold,
						color = VideoMaxTheme.extended.textPrimary
					)
				},
				actions = {
					IconButton(onClick = { viewModel.setSearchOpen(!uiState.isSearchOpen) }) {
						Icon(
							imageVector = Icons.Default.Search,
							contentDescription = "Buscar",
							tint = VideoMaxTheme.extended.textPrimary
						)
					}
					Box {
						IconButton(onClick = { sortMenuExpanded = true }) {
							Icon(
								imageVector = Icons.AutoMirrored.Filled.Sort,
								contentDescription = "Filtros",
								tint = VideoMaxTheme.extended.textPrimary
							)
						}
						DropdownMenu(
							expanded = sortMenuExpanded,
							onDismissRequest = { sortMenuExpanded = false },
							shape = RoundedCornerShape(16.dp),
							containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
						) {
							val availableSortOptions = listOf(
								MusicSortOption.DATE_DESC to "Más recientes",
								MusicSortOption.TITLE_DESC to "Título Z-A",
								MusicSortOption.ALBUM_ASC to "Álbum",
								MusicSortOption.ALBUM_DESC to "Álbum Z-A",
								MusicSortOption.ARTIST_DESC to "Artista Z-A",
								MusicSortOption.DURATION_ASC to "Más cortos",
								MusicSortOption.DURATION_DESC to "Más largos",
								MusicSortOption.SIZE_ASC to "Más livianos",
								MusicSortOption.SIZE_DESC to "Más pesados",
								MusicSortOption.TRACK_NUMBER_ASC to "Pista",
								MusicSortOption.TRACK_NUMBER_DESC to "Pista inversa"
							)
							availableSortOptions.forEach { (option, label) ->
								DropdownMenuItem(
									text = { Text(label) },
									onClick = {
										viewModel.setSortOption(option)
										sortMenuExpanded = false
									}
								)
							}
						}
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
			)

			if (uiState.isSearchOpen) {
				SearchBar(
					query = uiState.query,
					onQueryChange = { viewModel.setQuery(it) },
					onSearch = { },
					active = false,
					onActiveChange = { },
					placeholder = { Text("Buscar canciones...") },
					leadingIcon = {
						Icon(Icons.Default.Search, contentDescription = null)
					},
					trailingIcon = {
						if (uiState.query.isNotEmpty()) {
							IconButton(onClick = { viewModel.setQuery("") }) {
								Icon(Icons.Default.Close, contentDescription = "Limpiar")
							}
						}
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = VideoMaxDimens.spacingLg)
				) {}
			}

			MusicTabRow(
				selectedTab = uiState.selectedTab,
				onTabSelected = { viewModel.setSelectedTab(it) }
			)

			PullToRefreshBox(
				isRefreshing = uiState.isScanning,
				onRefresh = { viewModel.scanLibrary() },
				modifier = Modifier.fillMaxSize()
			) {
				if (uiState.isScanning) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = VideoMaxDimens.spacingLg)
					) {
						Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
						androidx.compose.material3.LinearProgressIndicator(
							progress = { uiState.scanProgress },
							modifier = Modifier
								.fillMaxWidth()
								.height(4.dp)
								.clip(RoundedCornerShape(VideoMaxDimens.radiusFull)),
							color = MaterialTheme.colorScheme.primary,
							trackColor = MaterialTheme.colorScheme.surfaceVariant
						)
						Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXs))
						Text(
							text = "Escaneando... ${uiState.songCount} canciones",
							style = MaterialTheme.typography.labelSmall,
							color = VideoMaxTheme.extended.textTertiary
						)
					}
				}

			AnimatedContent(
					targetState = uiState.selectedTab,
					label = "tabContent"
				) { tab ->
					when (tab) {
						MusicTab.SONGS -> SongsTab(
							songs = lazySongs,
							uiState = uiState,
							adsEnabled = adsEnabled,
							onSongClick = { song, index ->
								// Snapshot guarded: itemCount/get can disagree mid-refresh.
								val allSongs = runCatching {
									(0 until lazySongs.itemCount).mapNotNull {
										runCatching { lazySongs[it] }.getOrNull()
									}
								}.getOrDefault(emptyList())
								startPlayback(allSongs, index)
							},
							onSongFavorite = { viewModel.toggleFavorite(it) },
							onTrackMore = { trackSheetSong = it },
							onPlayNext = { BackgroundAudioManager.playNext(it.toQueueItem()) },
							onOpenQueue = { showQueueSheet = true },
							onSortChange = { viewModel.setSortOption(it) },
							onClearFolder = { viewModel.clearFolderFilter() }
						)
						MusicTab.ALBUMS -> AlbumsTab(
							albums = uiState.albums,
							onAlbumClick = { album ->
								viewModel.setQuery(album.name)
								viewModel.setSortOption(MusicSortOption.TITLE_ASC)
								viewModel.setSelectedTab(MusicTab.SONGS)
							},
							onPlayAlbum = { album ->
								scope.launch { startPlayback(viewModel.albumSongs(album.albumId), 0) }
							},
							onShuffleAlbum = { album ->
								scope.launch { startPlayback(viewModel.albumSongs(album.albumId).shuffled(), 0) }
							}
						)
						MusicTab.ARTISTS -> ArtistsTab(
							artists = uiState.artists,
							onArtistClick = { artist ->
								viewModel.setQuery(artist.name)
								viewModel.setSortOption(MusicSortOption.TITLE_ASC)
								viewModel.setSelectedTab(MusicTab.SONGS)
							},
							onPlayArtist = { artist ->
								scope.launch { startPlayback(viewModel.artistSongs(artist.name), 0) }
							},
							onShuffleArtist = { artist ->
								scope.launch { startPlayback(viewModel.artistSongs(artist.name).shuffled(), 0) }
							}
						)
						MusicTab.FOLDERS -> FoldersTab(
							folders = uiState.folders,
							onFolderClick = { viewModel.selectFolderAndShowSongs(it) }
						)
					}
				}
			}
		}

		// ── Namida-style sheets ──────────────────────────────────
		trackSheetSong?.let { sheetSong ->
			TrackActionsSheet(
				song = sheetSong,
				onDismiss = { trackSheetSong = null },
				onPlayNext = {
					BackgroundAudioManager.playNext(sheetSong.toQueueItem())
					trackSheetSong = null
				},
				onAddToQueue = {
					BackgroundAudioManager.addToQueue(sheetSong.toQueueItem())
					trackSheetSong = null
				},
				onToggleFavorite = {
					viewModel.toggleFavorite(sheetSong)
					trackSheetSong = null
				},
				onShare = {
					shareSong(sheetSong)
					trackSheetSong = null
				}
			)
		}
		if (showQueueSheet) {
			MusicQueueSheet(
				onDismiss = { showQueueSheet = false },
				onPlayAt = { index ->
					BackgroundAudioManager.jumpTo(index)
					ensureAudioService()
				}
			)
		}
	}
}

@Composable
private fun MusicTabRow(
	selectedTab: MusicTab,
	onTabSelected: (MusicTab) -> Unit
) {
	val tabs = MusicTab.entries
	TabRow(
		selectedTabIndex = tabs.indexOf(selectedTab),
		containerColor = Color.Transparent,
		contentColor = MaterialTheme.colorScheme.primary,
		indicator = { tabPositions ->
			if (tabs.indexOf(selectedTab) < tabPositions.size) {
				SecondaryIndicator(
					modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
					color = MaterialTheme.colorScheme.primary
				)
			}
		}
	) {
		tabs.forEach { tab ->
			Tab(
				selected = selectedTab == tab,
				onClick = { onTabSelected(tab) },
				text = {
					Text(
						text = when (tab) {
							MusicTab.SONGS -> "Canciones"
							MusicTab.ALBUMS -> "Álbumes"
							MusicTab.ARTISTS -> "Artistas"
							MusicTab.FOLDERS -> "Carpetas"
						},
						style = MaterialTheme.typography.labelMedium
					)
				},
				icon = {
					Icon(
						imageVector = when (tab) {
							MusicTab.SONGS -> Icons.Default.MusicNote
							MusicTab.ALBUMS -> Icons.Default.Album
							MusicTab.ARTISTS -> Icons.Default.Person
							MusicTab.FOLDERS -> Icons.Default.Folder
						},
						contentDescription = null,
						modifier = Modifier.size(18.dp)
					)
				}
			)
		}
	}
}

@Composable
private fun SongsTab(
	songs: androidx.paging.compose.LazyPagingItems<Song>,
	uiState: MusicUiState,
	onSongClick: (Song, Int) -> Unit,
	onSongFavorite: (Song) -> Unit,
	onTrackMore: (Song) -> Unit,
	onPlayNext: (Song) -> Unit,
	onOpenQueue: () -> Unit,
	onSortChange: (MusicSortOption) -> Unit,
	onClearFolder: () -> Unit = {},
	adsEnabled: Boolean = true
) {
	// AUDIT(BAJA): current() reads .value without subscribing — the playing
	// highlight went stale on auto-advance until an unrelated recomposition.
	// Collect index+queue so the highlight follows the player reactively.
	val queueState by BackgroundAudioManager.queue.collectAsState()
	val currentIdx by BackgroundAudioManager.currentIndex.collectAsState()
	val currentPlayingId = queueState.getOrNull(currentIdx)?.videoId ?: 0L
	val isPlaying by BackgroundAudioManager.isPlaying.collectAsState()

	Column(modifier = Modifier.fillMaxSize()) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "${uiState.songCount} canciones",
				style = MaterialTheme.typography.labelMedium,
				color = VideoMaxTheme.extended.textTertiary,
				modifier = Modifier.weight(1f)
			)
			// Namida queue shortcut.
			IconButton(onClick = onOpenQueue, modifier = Modifier.size(36.dp)) {
				Icon(
					imageVector = Icons.Default.QueueMusic,
					contentDescription = "Ver cola",
					tint = VideoMaxTheme.extended.textPrimary
				)
			}
		}

		if (uiState.selectedFolder != null) {
			val folderDisplayName = uiState.selectedFolder.substringAfterLast('/', uiState.selectedFolder)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingXs),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					imageVector = Icons.Default.Folder,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(18.dp)
				)
				Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
				Text(
					text = folderDisplayName,
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.weight(1f)
				)
				Icon(
					imageVector = Icons.Default.Close,
					contentDescription = "Quitar filtro",
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier
						.size(18.dp)
						.clickable { onClearFolder() }
				)
			}
		}

		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))

		if (songs.itemCount == 0 && !uiState.isScanning) {
			EmptyTabState("Sin canciones", "Escaneá tu biblioteca para ver canciones")
		} else {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXs),
				contentPadding = androidx.compose.foundation.layout.PaddingValues(
					horizontal = VideoMaxDimens.spacingLg,
					vertical = VideoMaxDimens.spacingSm
				)
			) {
				// Sin Premium/pase no se reserva ningún slot de nativo: la lista
				// es 100% canciones, sin huecos fantasma (los slots vacíos igual
				// consumían spacing).
				val showAds = adsEnabled
				val totalCount = if (showAds) NativeAdPlacer.totalWithAds(songs.itemCount) else songs.itemCount
				// Clave estable por id + contentType: el toggle de favorito
				// actualiza solo isFavorite vía diff, sin reordenar ni mover
				// el scroll; los slots de anuncio nunca reciclan filas de canción.
				val songKey = songs.itemKey { song: Song -> "song_${song.id}" }
				items(
					count = totalCount,
					key = { position ->
						if (showAds && NativeAdPlacer.isAdPosition(position)) {
							"native_ad_$position"
						} else {
							// AUDIT-CRASH #2: idem Library — peek con fallback
							// posicional; get()/peek() directos tiran FATAL.
							val contentIdx = if (showAds) NativeAdPlacer.contentIndexFor(position) else position
							runCatching {
								songKey(contentIdx)
							}.getOrNull() ?: "song_pos_$contentIdx"
						}
					},
					contentType = { position ->
						if (showAds && NativeAdPlacer.isAdPosition(position)) "native_ad" else "song"
					}
				) { position ->
					if (showAds && NativeAdPlacer.isAdPosition(position)) {
						NativeAdSlot()
						return@items
					}
					val contentIdx = if (showAds) NativeAdPlacer.contentIndexFor(position) else position
					val song = runCatching { songs[contentIdx] }.getOrNull()
						?: return@items
					SongRow(
						song = song,
						listIndex = position,
						isCurrentlyPlaying = song.id == currentPlayingId,
						isPlaying = isPlaying,
						onClick = { onSongClick(song, contentIdx) },
						onFavorite = { onSongFavorite(song) },
						onPlayNext = { onPlayNext(song) },
						onMore = { onTrackMore(song) }
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRow(
	song: Song,
	listIndex: Int,
	isCurrentlyPlaying: Boolean,
	isPlaying: Boolean,
	onClick: () -> Unit,
	onFavorite: () -> Unit,
	onPlayNext: () -> Unit,
	onMore: () -> Unit
) {
	val context = LocalContext.current
	// Namida quick swipe: right = favorite, left = play next. Returning false
	// snaps back (action, not dismissal).
	val dismissState = rememberSwipeToDismissBoxState(
		confirmValueChange = { value ->
			when (value) {
				SwipeToDismissBoxValue.StartToEnd -> {
					onFavorite()
					false
				}
				SwipeToDismissBoxValue.EndToStart -> {
					onPlayNext()
					false
				}
				SwipeToDismissBoxValue.Settled -> false
			}
		}
	)
	// Third row like Namida's configurable TrackTileItem stats line.
	val statsLine = listOfNotNull(
		song.durationLabel,
		song.year.takeIf { it > 0 }?.toString(),
		"▶ ${song.playCount}".takeIf { song.playCount > 0 },
		song.formatLabel
	).joinToString(" · ")

	SwipeToDismissBox(
		state = dismissState,
		backgroundContent = {
			SwipeActionBackground(direction = dismissState.dismissDirection)
		},
		content = {
			// Staggered entrance (flutter_staggered_animations equivalent).
			AnimatedVisibility(
				visible = true,
				enter = fadeIn(tween(220, delayMillis = (listIndex % 10) * 30)),
				label = "songRow_$listIndex"
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
						.background(
							if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer
							else Color.Transparent
						)
						.clickable(onClick = onClick)
						.padding(vertical = VideoMaxDimens.spacingSm, horizontal = VideoMaxDimens.spacingXs),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(52.dp)
							.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
							.background(MaterialTheme.colorScheme.surfaceContainerHigh),
						contentAlignment = Alignment.Center
					) {
						var artFailed by androidx.compose.runtime.remember(song.id) {
							androidx.compose.runtime.mutableStateOf(false)
						}
						val albumArtUri = "content://media/external/audio/albumart/${song.albumId}"
						if (!artFailed) {
							AsyncImage(
								model = ImageRequest.Builder(context)
									.data(albumArtUri)
									.crossfade(true)
									.build(),
								contentDescription = null,
								modifier = Modifier.fillMaxSize(),
								contentScale = ContentScale.Crop,
								onError = { artFailed = true }
							)
						}
						if (artFailed) {
							Text(
								text = song.title.firstOrNull()?.uppercase() ?: "♪",
								style = MaterialTheme.typography.titleMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
									alpha = 0.9f,
									red = ((song.id.hashCode() and 0xFF) / 255f * 0.25f + 0.55f).coerceIn(0f, 1f)
								)
							)
						}
						if (isCurrentlyPlaying && isPlaying) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.background(Color.Black.copy(alpha = 0.4f))
							)
							Icon(
								imageVector = Icons.Default.Pause,
								contentDescription = null,
								tint = Color.White,
								modifier = Modifier.size(24.dp)
							)
						}
					}
					Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = song.title,
							style = MaterialTheme.typography.bodyLarge,
							color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary
							else VideoMaxTheme.extended.textPrimary,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							text = "${song.artist} · ${song.album}",
							style = MaterialTheme.typography.bodySmall,
							color = VideoMaxTheme.extended.textTertiary,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							text = statsLine,
							style = MaterialTheme.typography.labelSmall,
							color = VideoMaxTheme.extended.textTertiary.copy(alpha = 0.8f),
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
					}
					IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
						Icon(
							imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
							contentDescription = "Favorito",
							tint = if (song.isFavorite) Color(0xFFE91E63) else VideoMaxTheme.extended.textTertiary,
							modifier = Modifier.size(20.dp)
						)
					}
					IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) {
						Icon(
							imageVector = Icons.Default.MoreVert,
							contentDescription = "Más acciones",
							tint = VideoMaxTheme.extended.textTertiary,
							modifier = Modifier.size(20.dp)
						)
					}
				}
			}
		}
	)
}

@Composable
fun SwipeActionBackground(direction: SwipeToDismissBoxValue) {
	val (icon, alignment, tint) = when (direction) {
		SwipeToDismissBoxValue.StartToEnd -> Triple(
			Icons.Default.Favorite, Alignment.CenterStart, Color(0xFFE91E63)
		)
		else -> Triple(
			Icons.Default.SkipNext, Alignment.CenterEnd, MaterialTheme.colorScheme.primary
		)
	}
	Box(
		modifier = Modifier
			.fillMaxSize()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
			.background(tint.copy(alpha = 0.15f))
			.padding(horizontal = VideoMaxDimens.spacingLg),
		contentAlignment = alignment
	) {
		Icon(imageVector = icon, contentDescription = null, tint = tint)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
	song: Song,
	onDismiss: () -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	onToggleFavorite: () -> Unit,
	onShare: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState()
	ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Box(
					modifier = Modifier
						.size(48.dp)
						.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
						.background(MaterialTheme.colorScheme.primaryContainer),
					contentAlignment = Alignment.Center
				) {
					Text(
						text = song.title.firstOrNull()?.uppercase() ?: "♪",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}
				Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = song.title,
						style = MaterialTheme.typography.titleSmall,
						color = VideoMaxTheme.extended.textPrimary,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					Text(
						text = "${song.artist} · ${song.durationLabel}",
						style = MaterialTheme.typography.bodySmall,
						color = VideoMaxTheme.extended.textTertiary,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			}
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingMd))
			SheetAction(icon = Icons.Default.SkipNext, label = "Reproducir siguiente", onClick = onPlayNext)
			SheetAction(icon = Icons.Default.PlaylistAdd, label = "Agregar a la cola", onClick = onAddToQueue)
			SheetAction(
				icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
				label = if (song.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
				onClick = onToggleFavorite
			)
			SheetAction(icon = Icons.Default.Share, label = "Compartir", onClick = onShare)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
		}
	}
}

@Composable
fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
			.clickable(onClick = onClick)
			.padding(vertical = VideoMaxDimens.spacingMd, horizontal = VideoMaxDimens.spacingSm),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(22.dp)
		)
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
		Text(
			text = label,
			style = MaterialTheme.typography.bodyLarge,
			color = VideoMaxTheme.extended.textPrimary
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicQueueSheet(
	onDismiss: () -> Unit,
	onPlayAt: (Int) -> Unit
) {
	val queue by BackgroundAudioManager.queue.collectAsState()
	val currentIndex by BackgroundAudioManager.currentIndex.collectAsState()
	val isPlaying by BackgroundAudioManager.isPlaying.collectAsState()
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
			Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
			Text(
				text = "Cola · ${queue.size} canciones",
				style = MaterialTheme.typography.titleSmall,
				color = VideoMaxTheme.extended.textPrimary,
				modifier = Modifier.weight(1f)
			)
		}
		LazyColumn(
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm)
		) {
			itemsIndexed(queue, key = { index, item -> "${item.videoId}_${index}_${item.uri}" }) { index, item ->
				val isCurrent = index == currentIndex
				val qDismiss = rememberSwipeToDismissBoxState(
					confirmValueChange = { value ->
						if (value == SwipeToDismissBoxValue.EndToStart) {
							BackgroundAudioManager.removeFromQueue(index)
						}
						false
					}
				)
				SwipeToDismissBox(
					state = qDismiss,
					backgroundContent = { SwipeActionBackground(SwipeToDismissBoxValue.EndToStart) },
					content = {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
								.background(
									if (isCurrent) MaterialTheme.colorScheme.primaryContainer
									else Color.Transparent
								)
								.clickable { onPlayAt(index) }
								.padding(vertical = VideoMaxDimens.spacingSm, horizontal = VideoMaxDimens.spacingSm),
							verticalAlignment = Alignment.CenterVertically
						) {
							if (isCurrent && isPlaying) {
								Icon(
									imageVector = Icons.Default.Pause,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.primary,
									modifier = Modifier.size(20.dp)
								)
							} else {
								Text(
									text = "${index + 1}",
									style = MaterialTheme.typography.labelMedium,
									color = VideoMaxTheme.extended.textTertiary,
									modifier = Modifier.width(24.dp)
								)
							}
							Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
							Column(modifier = Modifier.weight(1f)) {
								Text(
									text = item.displayName,
									style = MaterialTheme.typography.bodyMedium,
									color = if (isCurrent) MaterialTheme.colorScheme.primary
									else VideoMaxTheme.extended.textPrimary,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
								)
								if (item.artist.isNotEmpty()) {
									Text(
										text = item.artist,
										style = MaterialTheme.typography.bodySmall,
										color = VideoMaxTheme.extended.textTertiary,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis
									)
								}
							}
						}
					}
				)
			}
		}
		Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
	}
}

@Composable
private fun AlbumsTab(
	albums: List<Album>,
	onAlbumClick: (Album) -> Unit,
	onPlayAlbum: (Album) -> Unit,
	onShuffleAlbum: (Album) -> Unit
) {
	if (albums.isEmpty()) {
		EmptyTabState("Sin álbumes", "Escaneá tu biblioteca para ver álbumes")
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXs),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(VideoMaxDimens.spacingLg)
		) {
			items(albums) { album ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
						.clickable { onAlbumClick(album) }
						.padding(VideoMaxDimens.spacingMd),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(48.dp)
							.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
							.background(MaterialTheme.colorScheme.surfaceContainerHigh),
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.Default.Album,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(24.dp)
						)
					}
					Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = album.name,
							style = MaterialTheme.typography.bodyLarge,
							color = VideoMaxTheme.extended.textPrimary,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							text = "${album.artist} · ${album.songCount} canciones",
							style = MaterialTheme.typography.bodySmall,
							color = VideoMaxTheme.extended.textTertiary
						)
					}
					// Namida album-header pattern: play-all + shuffle.
					IconButton(onClick = { onPlayAlbum(album) }, modifier = Modifier.size(40.dp)) {
						Icon(
							imageVector = Icons.Default.PlayArrow,
							contentDescription = "Reproducir álbum",
							tint = MaterialTheme.colorScheme.primary
						)
					}
					IconButton(onClick = { onShuffleAlbum(album) }, modifier = Modifier.size(40.dp)) {
						Icon(
							imageVector = Icons.Default.Shuffle,
							contentDescription = "Aleatorio",
							tint = VideoMaxTheme.extended.textTertiary
						)
					}
				}
			}
		}
	}
}

@Composable
private fun ArtistsTab(
	artists: List<Artist>,
	onArtistClick: (Artist) -> Unit,
	onPlayArtist: (Artist) -> Unit,
	onShuffleArtist: (Artist) -> Unit
) {
	if (artists.isEmpty()) {
		EmptyTabState("Sin artistas", "Escaneá tu biblioteca para ver artistas")
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXs),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(VideoMaxDimens.spacingLg)
		) {
			items(artists) { artist ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
						.clickable { onArtistClick(artist) }
						.padding(VideoMaxDimens.spacingMd),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(48.dp)
							.clip(CircleShape)
							.background(MaterialTheme.colorScheme.surfaceContainerHigh),
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.Default.Person,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(24.dp)
						)
					}
					Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = artist.name,
							style = MaterialTheme.typography.bodyLarge,
							color = VideoMaxTheme.extended.textPrimary,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							text = "${artist.albumCount} álbumes · ${artist.songCount} canciones",
							style = MaterialTheme.typography.bodySmall,
							color = VideoMaxTheme.extended.textTertiary
						)
					}
					IconButton(onClick = { onPlayArtist(artist) }, modifier = Modifier.size(40.dp)) {
						Icon(
							imageVector = Icons.Default.PlayArrow,
							contentDescription = "Reproducir artista",
							tint = MaterialTheme.colorScheme.primary
						)
					}
					IconButton(onClick = { onShuffleArtist(artist) }, modifier = Modifier.size(40.dp)) {
						Icon(
							imageVector = Icons.Default.Shuffle,
							contentDescription = "Aleatorio",
							tint = VideoMaxTheme.extended.textTertiary
						)
					}
				}
			}
		}
	}
}

@Composable
private fun FoldersTab(
	folders: List<String>,
	onFolderClick: (String) -> Unit
) {
	if (folders.isEmpty()) {
		EmptyTabState("Sin carpetas", "Escaneá tu biblioteca para ver carpetas")
	} else {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingXs),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(VideoMaxDimens.spacingLg)
		) {
			items(folders) { folder ->
				val displayName = folder.substringAfterLast('/', folder)
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
						.clickable { onFolderClick(folder) }
						.padding(VideoMaxDimens.spacingMd),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(48.dp)
							.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
							.background(MaterialTheme.colorScheme.surfaceContainerHigh),
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.Default.Folder,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(24.dp)
						)
					}
					Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
					Text(
						text = displayName,
						style = MaterialTheme.typography.bodyLarge,
						color = VideoMaxTheme.extended.textPrimary,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			}
		}
	}
}

@Composable
private fun EmptyTabState(title: String, subtitle: String) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(
				imageVector = Icons.Default.Headphones,
				contentDescription = null,
				modifier = Modifier.size(64.dp),
				tint = VideoMaxTheme.extended.textTertiary
			)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = VideoMaxTheme.extended.textPrimary
			)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodySmall,
				color = VideoMaxTheme.extended.textTertiary
			)
		}
	}
}
