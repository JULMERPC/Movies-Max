package com.example.videomax.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.usecase.ObserveFavoritesUseCase
import com.example.videomax.domain.usecase.ToggleFavoriteUseCase
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoGridItem
import com.example.videomax.presentation.player.PlaybackQueue
import com.example.videomax.presentation.theme.VideoMaxDimens
import com.example.videomax.presentation.theme.VideoMaxTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
	observeFavorites: ObserveFavoritesUseCase,
	private val toggleFavorite: ToggleFavoriteUseCase,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	val favorites: StateFlow<List<Video>> = observeFavorites()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun toggle(videoId: Long) {
		viewModelScope.launch { toggleFavorite(videoId) }
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

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text("Favoritos", color = VideoMaxTheme.extended.textPrimary) },
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
		if (favorites.isEmpty()) {
			EmptyState(
				title = "Sin favoritos",
				subtitle = "Tocá el corazón en cualquier video para fijarlo acá.",
				modifier = Modifier.padding(padding)
			)
		} else {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(168.dp),
				contentPadding = PaddingValues(VideoMaxDimens.spacingLg),
				verticalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingLg),
				horizontalArrangement = Arrangement.spacedBy(VideoMaxDimens.spacingMd),
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
			) {
				items(favorites, key = { it.id }) { video ->
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
}
