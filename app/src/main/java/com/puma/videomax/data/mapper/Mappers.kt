package com.puma.videomax.data.mapper

import com.puma.videomax.data.local.db.dao.AlbumInfo
import com.puma.videomax.data.local.db.dao.ArtistInfo
import com.puma.videomax.data.local.db.dao.PlaylistWithCount
import com.puma.videomax.data.local.db.entity.HistoryEntity
import com.puma.videomax.data.local.db.entity.PlaylistEntity
import com.puma.videomax.data.local.db.entity.SongEntity
import com.puma.videomax.data.local.db.entity.VideoEntity
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.PlaybackHistory
import com.puma.videomax.domain.model.Playlist
import com.puma.videomax.domain.model.Song
import com.puma.videomax.domain.model.Video

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
	playCount = playCount,
	isNew = isNew
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
	playCount = playCount,
	isNew = isNew
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

fun SongEntity.toDomain(): Song = Song(
	id = id,
	uri = uri,
	title = title,
	artist = artist,
	album = album,
	albumId = albumId,
	durationMs = durationMs,
	sizeBytes = sizeBytes,
	mimeType = mimeType,
	dateAdded = dateAdded,
	dateModified = dateModified,
	path = path,
	folderName = folderName,
	trackNumber = trackNumber,
	discNumber = discNumber,
	year = year,
	bitrate = bitrate,
	sampleRate = sampleRate,
	isFavorite = isFavorite,
	lastPositionMs = lastPositionMs,
	playCount = playCount,
	trackGain = trackGain,
	albumGain = albumGain,
	trackPeak = trackPeak,
	albumPeak = albumPeak
)

fun Song.toEntity(): SongEntity = SongEntity(
	id = id,
	uri = uri,
	title = title,
	artist = artist,
	album = album,
	albumId = albumId,
	durationMs = durationMs,
	sizeBytes = sizeBytes,
	mimeType = mimeType,
	dateAdded = dateAdded,
	dateModified = dateModified,
	path = path,
	folderName = folderName,
	trackNumber = trackNumber,
	discNumber = discNumber,
	year = year,
	bitrate = bitrate,
	sampleRate = sampleRate,
	isFavorite = isFavorite,
	lastPositionMs = lastPositionMs,
	playCount = playCount,
	trackGain = trackGain,
	albumGain = albumGain,
	trackPeak = trackPeak,
	albumPeak = albumPeak
)

fun AlbumInfo.toDomain(): Album = Album(
	albumId = albumId,
	name = album,
	artist = artist,
	songCount = songCount,
	dateAdded = dateAdded
)

fun ArtistInfo.toDomain(): Artist = Artist(
	name = artist,
	songCount = songCount,
	albumCount = albumCount
)
