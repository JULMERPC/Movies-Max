package com.example.videomax.presentation.history

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.PlaybackHistory
import com.example.videomax.domain.repository.HistoryRepository
import com.example.videomax.domain.usecase.ObserveHistoryUseCase
import com.example.videomax.presentation.components.EmptyState
import com.example.videomax.presentation.components.VideoThumbnail
import com.example.videomax.presentation.player.PlaybackQueue
import com.example.videomax.util.Formatters
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
		topBar = {
			TopAppBar(
				title = { Text("History") },
				actions = {
					if (history.isNotEmpty()) {
						IconButton(onClick = viewModel::clear) {
							Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar historial")
						}
					}
				}
			)
		}
	) { padding ->
		if (history.isEmpty()) {
			EmptyState(
				title = "No playback history",
				subtitle = "Videos you watch will appear here with resume progress.",
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
							.padding(horizontal = 16.dp, vertical = 10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						VideoThumbnail(
							uri = item.videoUri,
							modifier = Modifier
								.width(120.dp)
								.height(68.dp)
								.clip(RoundedCornerShape(10.dp))
						)
						Spacer(modifier = Modifier.width(12.dp))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = item.displayName,
								style = MaterialTheme.typography.titleMedium,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis
							)
							Text(
								text = "${Formatters.formatDuration(item.positionMs)} / ${Formatters.formatDuration(item.durationMs)}",
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Spacer(modifier = Modifier.height(6.dp))
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
