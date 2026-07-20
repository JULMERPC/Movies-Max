package com.example.videomax.domain.model

data class Playlist(
	val id: Long = 0,
	val name: String,
	val createdAt: Long = System.currentTimeMillis(),
	val videoCount: Int = 0
)

data class PlaylistWithVideos(
	val playlist: Playlist,
	val videos: List<Video>
)
