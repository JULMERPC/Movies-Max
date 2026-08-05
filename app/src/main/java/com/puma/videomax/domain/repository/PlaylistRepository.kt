package com.puma.videomax.domain.repository

import com.puma.videomax.domain.model.Playlist
import com.puma.videomax.domain.model.PlaylistWithVideos
import com.puma.videomax.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
	fun observePlaylists(): Flow<List<Playlist>>
	fun observePlaylistWithVideos(playlistId: Long): Flow<PlaylistWithVideos?>
	suspend fun createPlaylist(name: String): Long
	suspend fun deletePlaylist(playlistId: Long)
	suspend fun renamePlaylist(playlistId: Long, name: String)
	suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long)
	suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long)
	suspend fun getPlaylistVideos(playlistId: Long): List<Video>
}
