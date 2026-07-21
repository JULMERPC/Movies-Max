package com.example.videomax.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.videomax.data.local.db.entity.VideoEntity
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
 * Scans device videos via MediaStore in incremental batches on [Dispatchers.IO].
 * Emits each batch as soon as it is ready so Room/UI can update progressively.
 */
@Singleton
class MediaStoreVideoScanner @Inject constructor(
	@param:ApplicationContext private val context: Context
) {

	/**
	 * Streams MediaStore rows in batches. Never accumulates the full library in memory.
	 *
	 * @param batchSize number of videos per emission (typical: 48–80)
	 */
	fun scanBatches(batchSize: Int = 64): Flow<ScanBatch> = flow {
		val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		} else {
			MediaStore.Video.Media.EXTERNAL_CONTENT_URI
		}

		val projection = arrayOf(
			MediaStore.Video.Media._ID,
			MediaStore.Video.Media.DISPLAY_NAME,
			MediaStore.Video.Media.DURATION,
			MediaStore.Video.Media.SIZE,
			MediaStore.Video.Media.WIDTH,
			MediaStore.Video.Media.HEIGHT,
			MediaStore.Video.Media.MIME_TYPE,
			MediaStore.Video.Media.DATE_ADDED,
			MediaStore.Video.Media.DATE_MODIFIED,
			MediaStore.Video.Media.DATA,
			MediaStore.Video.Media.BUCKET_DISPLAY_NAME
		)

		val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

		context.contentResolver.query(
			collection,
			projection,
			null,
			null,
			sortOrder
		)?.use { cursor ->
			val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
			val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
			val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
			val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
			val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
			val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
			val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
			val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
			val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
			val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
			val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

			val totalHint = cursor.count.coerceAtLeast(0)
			val batch = ArrayList<VideoEntity>(batchSize)

			while (cursor.moveToNext()) {
				val id = cursor.getLong(idCol)
				val uri = ContentUris.withAppendedId(collection, id).toString()
				val path = cursor.getString(dataCol)
				val folder = cursor.getString(bucketCol)
					?: path?.let { File(it).parentFile?.name }
					?: "Internal"
				val mime = cursor.getString(mimeCol)

				batch += VideoEntity(
					id = id,
					uri = uri,
					displayName = cursor.getString(nameCol) ?: "Video $id",
					path = path,
					durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
					sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
					width = cursor.getInt(widthCol).coerceAtLeast(0),
					height = cursor.getInt(heightCol).coerceAtLeast(0),
					mimeType = mime ?: "video/*",
					dateAdded = cursor.getLong(addedCol) * 1000L,
					dateModified = cursor.getLong(modifiedCol) * 1000L,
					folderName = folder,
					codec = guessCodec(mime)
				)

				if (batch.size >= batchSize) {
					emit(ScanBatch(videos = batch.toList(), totalHint = totalHint))
					batch.clear()
					yield()
				}
			}

			if (batch.isNotEmpty()) {
				emit(ScanBatch(videos = batch.toList(), totalHint = totalHint))
			}
		}
	}.flowOn(Dispatchers.IO)

	private fun guessCodec(mimeType: String?): String? = when {
		mimeType == null -> null
		mimeType.contains("mp4", ignoreCase = true) -> "H.264 / AAC (MP4)"
		mimeType.contains("webm", ignoreCase = true) -> "VP9 / Opus (WebM)"
		mimeType.contains("matroska", ignoreCase = true) ||
			mimeType.contains("mkv", ignoreCase = true) -> "Matroska"
		mimeType.contains("3gpp", ignoreCase = true) -> "H.263 / AMR"
		mimeType.contains("avi", ignoreCase = true) -> "AVI"
		else -> mimeType.substringAfter('/', mimeType).uppercase()
	}
}

data class ScanBatch(
	val videos: List<VideoEntity>,
	val totalHint: Int
)
