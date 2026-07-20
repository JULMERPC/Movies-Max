package com.example.videomax.data.mapper

import com.example.videomax.data.local.db.dao.PlaylistWithCount
import com.example.videomax.data.local.db.entity.HistoryEntity
import com.example.videomax.data.local.db.entity.PlaylistEntity
import com.example.videomax.data.local.db.entity.VideoEntity
import com.example.videomax.domain.model.PlaybackHistory
import com.example.videomax.domain.model.Playlist
import com.example.videomax.domain.model.Video

fun VideoEntity.toDomain(): Video = Video(
	id = id,
	uri = uri,
	displayName = displayName,
	path = path,
	durationMs = durationMs,
	sizeBytes = sizeBytes,
	width = width,
	height = height,
	mimeType = mimeType,
	dateAdded = dateAdded,
	dateModified = dateModified,
	folderName = folderName,
	isFavorite = isFavorite,
	lastPositionMs = lastPositionMs,
	codec = codec,
	playCount = playCount
)

fun Video.toEntity(): VideoEntity = VideoEntity(
	id = id,
	uri = uri,
	displayName = displayName,
	path = path,
	durationMs = durationMs,
	sizeBytes = sizeBytes,
	width = width,
	height = height,
	mimeType = mimeType,
	dateAdded = dateAdded,
	dateModified = dateModified,
	folderName = folderName,
	isFavorite = isFavorite,
	lastPositionMs = lastPositionMs,
	codec = codec,
	playCount = playCount
)

fun PlaylistWithCount.toDomain(): Playlist = Playlist(
	id = id,
	name = name,
	createdAt = createdAt,
	videoCount = videoCount
)

fun PlaylistEntity.toDomain(videoCount: Int = 0): Playlist = Playlist(
	id = id,
	name = name,
	createdAt = createdAt,
	videoCount = videoCount
)

fun HistoryEntity.toDomain(): PlaybackHistory = PlaybackHistory(
	videoId = videoId,
	videoUri = videoUri,
	displayName = displayName,
	positionMs = positionMs,
	durationMs = durationMs,
	watchedAt = watchedAt
)
