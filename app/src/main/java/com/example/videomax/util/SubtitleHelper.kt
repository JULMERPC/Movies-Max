package com.example.videomax.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.media3.common.MimeTypes
import com.example.videomax.domain.model.SubtitleTrack
import java.io.File

/**
 * Discovers sidecar subtitle files next to a video.
 *
 * **Blocking I/O** — always call from [kotlinx.coroutines.Dispatchers.IO].
 * Matches: `video.srt`, `video.en.srt`, `video.ass`, etc.
 */
object SubtitleHelper {

	private val supportedExtensions = setOf("srt", "vtt", "ass", "ssa")

	/**
	 * Finds external subtitles for a single video path.
	 * Prefers cheap existence checks for exact names, then a filtered directory listing
	 * for language-tagged sidecars (e.g. `Movie.en.srt`).
	 */
	fun findExternalSubtitles(videoPath: String?): List<SubtitleTrack> {
		if (videoPath.isNullOrBlank()) return emptyList()
		val videoFile = File(videoPath)
		val parent = videoFile.parentFile ?: return emptyList()
		if (!parent.isDirectory) return emptyList()
		val baseName = videoFile.nameWithoutExtension
		if (baseName.isBlank()) return emptyList()

		val found = LinkedHashMap<String, File>()

		// Fast path: exact basename + extension (most common case).
		for (ext in supportedExtensions) {
			val exact = File(parent, "$baseName.$ext")
			if (exact.isFile) {
				found[exact.absolutePath] = exact
			}
		}

		// Language / variant sidecars: basename.* but only matching extensions.
		val prefix = "$baseName."
		parent.listFiles()
			?.asSequence()
			?.filter { file ->
				file.isFile &&
					file.extension.lowercase() in supportedExtensions &&
					file.nameWithoutExtension.startsWith(baseName, ignoreCase = true) &&
					(file.nameWithoutExtension.equals(baseName, ignoreCase = true) ||
						file.name.startsWith(prefix, ignoreCase = true))
			}
			?.forEach { file -> found[file.absolutePath] = file }

		return found.values.map { file ->
			SubtitleTrack(
				uri = Uri.fromFile(file).toString(),
				label = file.name,
				mimeType = mimeForExtension(file.extension),
				language = null
			)
		}
	}

	fun mimeForUri(context: Context, uri: Uri): String {
		val type = context.contentResolver.getType(uri)
		if (!type.isNullOrBlank()) return type
		val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
			?: uri.lastPathSegment?.substringAfterLast('.', "")
			?: ""
		return mimeForExtension(extension)
	}

	fun mimeForExtension(extension: String): String = when (extension.lowercase()) {
		"srt" -> MimeTypes.APPLICATION_SUBRIP
		"vtt" -> MimeTypes.TEXT_VTT
		"ass", "ssa" -> MimeTypes.TEXT_SSA
		else -> MimeTypes.APPLICATION_SUBRIP
	}

	fun isSupportedSubtitle(uri: Uri): Boolean {
		val name = uri.lastPathSegment?.lowercase().orEmpty()
		return supportedExtensions.any { name.endsWith(".$it") }
	}
}
