package com.example.videomax.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.videomax.domain.model.SubtitleTrack
import androidx.media3.common.MimeTypes
import java.io.File

object SubtitleHelper {

	private val supportedExtensions = setOf("srt", "vtt", "ass", "ssa")

	fun findExternalSubtitles(videoPath: String?): List<SubtitleTrack> {
		if (videoPath.isNullOrBlank()) return emptyList()
		val videoFile = File(videoPath)
		val parent = videoFile.parentFile ?: return emptyList()
		val baseName = videoFile.nameWithoutExtension

		return parent.listFiles()
			?.asSequence()
			?.filter { file ->
				file.isFile &&
					file.extension.lowercase() in supportedExtensions &&
					file.nameWithoutExtension.startsWith(baseName, ignoreCase = true)
			}
			?.map { file ->
				SubtitleTrack(
					uri = Uri.fromFile(file).toString(),
					label = file.name,
					mimeType = mimeForExtension(file.extension),
					language = null
				)
			}
			?.toList()
			.orEmpty()
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
