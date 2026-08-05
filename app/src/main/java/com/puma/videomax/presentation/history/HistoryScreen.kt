package com.puma.videomax.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.puma.videomax.domain.model.PlaybackHistory
import com.puma.videomax.domain.repository.HistoryRepository
import com.puma.videomax.domain.usecase.ObserveHistoryUseCase
import com.puma.videomax.presentation.components.EmptyState
import com.puma.videomax.presentation.components.VideoThumbnail
import com.puma.videomax.presentation.player.PlaybackQueue
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme
import com.puma.videomax.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
	observeHistory: ObserveHistoryUseCase,
	private val historyRepository: HistoryRepository,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	val history: StateFlow<List<PlaybackHistory>> = observeHistory()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun clear() {
		viewModelScope.launch { historyRepository.clearHistory() }
	}

	fun preparePlayback(videoId: Long) {
		playbackQueue.setQueue(history.value.map { it.videoId }, videoId)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
	onOpenPlayer: (Long) -> Unit,
	viewModel: HistoryViewModel = hiltViewModel()
) {
	val history by viewModel.history.collectAsStateWithLifecycle()

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = { Text("Historial", color = VideoMaxTheme.extended.textPrimary) },
				actions = {
					if (history.isNotEmpty()) {
						IconButton(onClick = viewModel::clear) {
							Icon(
								Icons.Default.DeleteSweep,
								contentDescription = "Limpiar historial",
								tint = VideoMaxTheme.extended.textTertiary
							)
						}
					}
				},
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
		if (history.isEmpty()) {
			EmptyState(
				title = "Sin historial",
				subtitle = "Los videos que veas aparecerán acá con progreso de reproducción.",
				modifier = Modifier.padding(padding)
			)
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
			) {
				items(history, key = { it.videoId }) { item ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable {
								viewModel.preparePlayback(item.videoId)
								onOpenPlayer(item.videoId)
							}
							.padding(horizontal = VideoMaxDimens.spacingLg, vertical = VideoMaxDimens.spacingSm),
						verticalAlignment = Alignment.CenterVertically
					) {
						VideoThumbnail(
							uri = item.videoUri,
							modifier = Modifier
								.width(120.dp)
								.height(68.dp)
								.clip(RoundedCornerShape(VideoMaxDimens.radiusSm))
						)
						Spacer(modifier = Modifier.width(VideoMaxDimens.spacingMd))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = item.displayName,
								style = MaterialTheme.typography.titleMedium,
								color = VideoMaxTheme.extended.textPrimary,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis
							)
							Text(
								text = "${Formatters.formatDuration(item.positionMs)} / ${Formatters.formatDuration(item.durationMs)}",
								style = MaterialTheme.typography.bodyMedium,
								color = VideoMaxTheme.extended.textTertiary
							)
							Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXs))
							LinearProgressIndicator(
								progress = { item.progressFraction },
								modifier = Modifier.fillMaxWidth()
							)
						}
					}
				}
			}
		}
	}
}
