package com.puma.videomax.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class EmbeddedTags(
	val title: String?,
	val artist: String?,
	val album: String?,
	val pictureBytes: ByteArray?,
	val durationMs: Long,
	val mime: String?,
)

/**
 * Direct tag extraction from the file (Namida: `NamidaTaggerController` /
 * TagLib first, FFmpeg second). Never throws: returns null on any failure
 * instead of an uncontrolled `IOException`, like `FAudioModel.dummy()`.
 */
@Singleton
class LocalTagExtractor @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	suspend fun extract(path: String?, uri: String): EmbeddedTags? = withContext(Dispatchers.IO) {
		if (path.isNullOrBlank()) return@withContext null
		val retriever = MediaMetadataRetriever()
		try {
			retriever.setDataSource(path)
			EmbeddedTags(
				title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
				artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
				album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
				pictureBytes = runCatching { retriever.embeddedPicture }.getOrNull(),
				durationMs = retriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
					?.toLongOrNull() ?: 0L,
				mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
			)
		} catch (_: Exception) {
			null
		} finally {
			runCatching { retriever.release() }
		}
	}
}
