package com.puma.videomax.data.repository

import com.puma.videomax.data.local.db.dao.PlaylistDao
import com.puma.videomax.data.local.db.entity.PlaylistEntity
import com.puma.videomax.data.local.db.entity.PlaylistVideoCrossRef
import com.puma.videomax.data.mapper.toDomain
import com.puma.videomax.domain.model.Playlist
import com.puma.videomax.domain.model.PlaylistWithVideos
import com.puma.videomax.domain.model.Video
import com.puma.videomax.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
	private val playlistDao: PlaylistDao
) : PlaylistRepository {

	override fun observePlaylists(): Flow<List<Playlist>> =
		playlistDao.observePlaylists().map { list -> list.map { it.toDomain() } }

	override fun observePlaylistWithVideos(playlistId: Long): Flow<PlaylistWithVideos?> =
		combine(
			playlistDao.observePlaylist(playlistId),
			playlistDao.observeVideos(playlistId)
		) { playlist, videos ->
			playlist?.let {
				PlaylistWithVideos(
					playlist = it.toDomain(videos.size),
					videos = videos.map { video -> video.toDomain() }
				)
			}
		}

	override suspend fun createPlaylist(name: String): Long =
		playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))

	override suspend fun deletePlaylist(playlistId: Long) {
		playlistDao.deletePlaylist(playlistId)
	}

	override suspend fun renamePlaylist(playlistId: Long, name: String) {
		playlistDao.renamePlaylist(playlistId, name.trim())
	}

	override suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long) {
		playlistDao.addVideo(PlaylistVideoCrossRef(playlistId = playlistId, videoId = videoId))
	}

	override suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
		playlistDao.removeVideo(playlistId, videoId)
	}

	override suspend fun getPlaylistVideos(playlistId: Long): List<Video> =
		playlistDao.getVideos(playlistId).map { it.toDomain() }
}
