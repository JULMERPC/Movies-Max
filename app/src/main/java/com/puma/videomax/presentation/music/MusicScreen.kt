package com.puma.videomax.presentation.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.presentation.theme.screenGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
	viewModel: MusicViewModel = hiltViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()
	val lazySongs = viewModel.songs.collectAsLazyPagingItems()
	val isPlaying by BackgroundAudioManager.isPlaying.collectAsState()
	val context = LocalContext.current
	var sortMenuExpanded by remember { mutableStateOf(false) }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(screenGradient())
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
							onSongClick = { song ->
								val item = AudioQueueItem(
									videoId = song.id,
									uri = song.uri,
									displayName = song.title,
									artist = song.artist,
									album = song.album,
									albumId = song.albumId
								)
								BackgroundAudioManager.playQueue(listOf(item), 0, true)
								val serviceIntent = Intent(context, BackgroundAudioService::class.java)
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
									context.startForegroundService(serviceIntent)
								} else {
									context.startService(serviceIntent)
								}
							},
							onSongFavorite = { viewModel.toggleFavorite(it) },
							onSortChange = { viewModel.setSortOption(it) },
							onClearFolder = { viewModel.clearFolderFilter() }
						)
						MusicTab.ALBUMS -> AlbumsTab(
							albums = uiState.albums,
							onAlbumClick = { }
						)
						MusicTab.ARTISTS -> ArtistsTab(
							artists = uiState.artists,
							onArtistClick = { }
						)
						MusicTab.FOLDERS -> FoldersTab(
							folders = uiState.folders,
							onFolderClick = { viewModel.selectFolderAndShowSongs(it) }
						)
					}
				}
			}
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
	onSongClick: (Song) -> Unit,
	onSongFavorite: (Song) -> Unit,
	onSortChange: (MusicSortOption) -> Unit,
	onClearFolder: () -> Unit = {}
) {
	val currentPlayingId = BackgroundAudioManager.current()?.videoId ?: 0L
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
				items(
					count = songs.itemCount,
					key = songs.itemKey { it.id }
				) { index ->
					val song = songs[index] ?: return@items
					SongRow(
						song = song,
						isCurrentlyPlaying = song.id == currentPlayingId,
						isPlaying = isPlaying,
						onClick = { onSongClick(song) },
						onFavorite = { onSongFavorite(song) }
					)
				}
			}
		}
	}
}

@Composable
	private fun SongRow(
	song: Song,
	isCurrentlyPlaying: Boolean,
	isPlaying: Boolean,
	onClick: () -> Unit,
	onFavorite: () -> Unit
) {
	val context = LocalContext.current
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(VideoMaxDimens.radiusMd))
			.clickable(onClick = onClick)
			.padding(vertical = VideoMaxDimens.spacingSm, horizontal = VideoMaxDimens.spacingXs),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(48.dp)
				.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
				.background(
					if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer
					else MaterialTheme.colorScheme.surfaceContainerHigh
				),
			contentAlignment = Alignment.Center
		) {
			val albumArtUri = "content://media/external/audio/albumart/${song.albumId}"
			AsyncImage(
				model = ImageRequest.Builder(context)
					.data(albumArtUri)
					.crossfade(true)
					.build(),
				contentDescription = null,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop
			)
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
				text = song.artist,
				style = MaterialTheme.typography.bodySmall,
				color = VideoMaxTheme.extended.textTertiary,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
		Text(
			text = song.formatLabel,
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.primary,
			modifier = Modifier
				.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
				.background(MaterialTheme.colorScheme.primaryContainer)
				.padding(horizontal = VideoMaxDimens.spacingXs, vertical = 2.dp)
		)
		Spacer(modifier = Modifier.width(VideoMaxDimens.spacingSm))
		Text(
			text = song.durationLabel,
			style = MaterialTheme.typography.labelMedium,
			color = VideoMaxTheme.extended.textTertiary
		)
	}
}

@Composable
private fun AlbumsTab(
	albums: List<Album>,
	onAlbumClick: (Album) -> Unit
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
				}
			}
		}
	}
}

@Composable
private fun ArtistsTab(
	artists: List<Artist>,
	onArtistClick: (Artist) -> Unit
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

private fun formatTime(ms: Long): String {
	val totalSeconds = ms / 1000
	val minutes = totalSeconds / 60
	val seconds = totalSeconds % 60
	return "%d:%02d".format(minutes, seconds)
}
