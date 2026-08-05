package com.puma.videomax.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.puma.videomax.data.local.db.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans device music via MediaStore in incremental batches on [Dispatchers.IO].
 * Emits each batch as soon as it is ready so Room/UI can update progressively.
 */
@Singleton
class MediaStoreMusicScanner @Inject constructor(
	@param:ApplicationContext private val context: Context
) {

	fun scanBatches(batchSize: Int = 64): Flow<MusicScanBatch> = flow {
		val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		} else {
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
		}

		val projection = arrayOf(
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.ALBUM_ID,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.SIZE,
			MediaStore.Audio.Media.MIME_TYPE,
			MediaStore.Audio.Media.DATE_ADDED,
			MediaStore.Audio.Media.DATE_MODIFIED,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.TRACK,
			MediaStore.Audio.Media.YEAR,
			MediaStore.Audio.Media.BITRATE
		)

		val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
		val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

		context.contentResolver.query(
			collection,
			projection,
			selection,
			null,
			sortOrder
		)?.use { cursor ->
			val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
			val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
			val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
			val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
			val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
			val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
			val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
			val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
			val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
			val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
			val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
			val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
			val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
			val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)

			val totalHint = cursor.count.coerceAtLeast(0)
			val batch = ArrayList<SongEntity>(batchSize)

			while (cursor.moveToNext()) {
				val id = cursor.getLong(idCol)
				val path = cursor.getString(dataCol)
				val uri = ContentUris.withAppendedId(collection, id).toString()
				val folder = path?.let { File(it).parentFile?.name } ?: "Internal"
				val mime = cursor.getString(mimeCol)
				val trackRaw = cursor.getInt(trackCol)
				val discNumber = trackRaw / 1000
				val trackNumber = trackRaw % 1000

				batch += SongEntity(
					id = id,
					uri = uri,
					title = cursor.getString(titleCol) ?: "Song $id",
					artist = cursor.getString(artistCol) ?: "<unknown>",
					album = cursor.getString(albumCol) ?: "<unknown>",
					albumId = cursor.getLong(albumIdCol).coerceAtLeast(-1L),
					durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
					sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
					mimeType = mime ?: "audio/mpeg",
					dateAdded = cursor.getLong(addedCol) * 1000L,
					dateModified = cursor.getLong(modifiedCol) * 1000L,
					path = path,
					folderName = folder,
					trackNumber = trackNumber.coerceAtLeast(0),
					discNumber = discNumber.coerceAtLeast(0),
					year = cursor.getInt(yearCol).coerceAtLeast(0),
					bitrate = cursor.getInt(bitrateCol).coerceAtLeast(0),
					sampleRate = 0
				)

				if (batch.size >= batchSize) {
					emit(MusicScanBatch(songs = batch.toList(), totalHint = totalHint))
					batch.clear()
					yield()
				}
			}

			if (batch.isNotEmpty()) {
				emit(MusicScanBatch(songs = batch.toList(), totalHint = totalHint))
			}
		}
	}.flowOn(Dispatchers.IO)
}

data class MusicScanBatch(
	val songs: List<SongEntity>,
	val totalHint: Int
)
