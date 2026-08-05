package com.puma.videomax.domain.model

data class Album(
	val albumId: Long,
	val name: String,
	val artist: String,
	val songCount: Int,
	val dateAdded: Long
)
