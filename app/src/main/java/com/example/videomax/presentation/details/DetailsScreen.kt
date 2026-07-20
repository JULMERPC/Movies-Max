package com.example.videomax.presentation.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.videomax.domain.model.Video
import com.example.videomax.domain.usecase.GetVideoByIdUseCase
import com.example.videomax.presentation.components.VideoThumbnail
import com.example.videomax.presentation.player.PlaybackQueue
import com.example.videomax.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val getVideoById: GetVideoByIdUseCase,
	private val playbackQueue: PlaybackQueue
) : ViewModel() {
	private val videoId: Long = checkNotNull(savedStateHandle["videoId"])
	private val _video = MutableStateFlow<Video?>(null)
	val video: StateFlow<Video?> = _video.asStateFlow()

	init {
		viewModelScope.launch {
			_video.value = getVideoById(videoId)
		}
	}

	fun preparePlayback() {
		playbackQueue.setQueue(listOf(videoId), videoId)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
	onBack: () -> Unit,
	onPlay: (Long) -> Unit,
	viewModel: DetailsViewModel = hiltViewModel()
) {
	val video by viewModel.video.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(video?.displayName ?: "Details") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		}
	) { padding ->
		val current = video ?: return@Scaffold
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(16.dp)
		) {
			VideoThumbnail(
				uri = current.uri,
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(16f / 9f)
					.clip(RoundedCornerShape(16.dp))
			)
			Spacer(modifier = Modifier.height(16.dp))
			Button(
				onClick = {
					viewModel.preparePlayback()
					onPlay(current.id)
				},
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(Icons.Default.PlayArrow, contentDescription = null)
				Spacer(modifier = Modifier.height(0.dp))
				Text("Play")
			}
			Spacer(modifier = Modifier.height(8.dp))
			InfoRow("Resolution", current.resolutionLabel)
			InfoRow("Duration", Formatters.formatDuration(current.durationMs))
			InfoRow("Size", Formatters.formatFileSize(current.sizeBytes))
			InfoRow("Codec / container", current.codec ?: current.mimeType)
			InfoRow("Folder", current.folderName)
			InfoRow("Location", current.path ?: current.uri)
			InfoRow("MIME", current.mimeType)
		}
	}
}

@Composable
private fun InfoRow(label: String, value: String) {
	ListItem(
		headlineContent = { Text(label) },
		supportingContent = { Text(value) }
	)
}
