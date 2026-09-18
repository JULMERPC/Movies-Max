package com.puma.videomax.data.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * 3-tier artwork strategy (Namida: `indexer_artwork_extract_strategies.dart`).
 *
 * 1. Memory/disk cache (`cacheDir/artworks/song_{id}.png`).
 * 2. Embedded tags via [LocalTagExtractor] (fast, direct bytes).
 * 3. `null` fallback — the UI draws a hash-based palette + title initial.
 *
 * Concurrent requests for the same track are deduplicated (Namida:
 * `_pendingArtworksFullRes[path] = Completer`), and no branch throws
 * `IOException` to the caller.
 */
@Singleton
class ArtworkRepository @Inject constructor(
	@ApplicationContext private val context: Context,
	private val extractor: LocalTagExtractor,
) {
	private val pending = ConcurrentHashMap<String, Deferred<File?>>()
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	suspend fun artworkFile(songId: Long, path: String?, uri: String): File? {
		val key = path?.takeIf { it.isNotBlank() } ?: uri
		pending[key]?.let { return it.await() }
		val deferred = scope.async {
			val dir = File(context.cacheDir, "artworks").apply { mkdirs() }
			val out = File(dir, "song_$songId.png")
			if (out.exists()) return@async out
			val bytes = runCatching { extractor.extract(path, uri)?.pictureBytes }.getOrNull()
				?: return@async null
			decodeAndPersist(bytes, out)
		}
		pending[key] = deferred
		return try {
			deferred.await()
		} finally {
			pending.remove(key)
		}
	}

	private fun decodeAndPersist(bytes: ByteArray, out: File): File? {
		if (bytes.isEmpty()) return null
		return runCatching {
			val options = BitmapFactory.Options().apply {
				inJustDecodeBounds = true
			}
			BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
			options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, 256)
			options.inJustDecodeBounds = false
			val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
				?: return null
			out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
			if (!bmp.isRecycled) bmp.recycle()
			out.takeIf { it.exists() }
		}.getOrElse {
			// AUDIT(BAJA): a failed write left a partial PNG that the
			// exists() fast-path then served forever. Remove it.
			runCatching { out.delete() }
			null
		}
	}

	private fun calculateSampleSize(w: Int, h: Int, target: Int): Int {
		if (w <= 0 || h <= 0) return 1
		var sample = 1
		while ((w / sample) > target * 2 || (h / sample) > target * 2) sample *= 2
		return sample
	}
}
