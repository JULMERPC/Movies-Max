package com.puma.videomax.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

	fun formatDuration(durationMs: Long): String {
		if (durationMs <= 0L) return "00:00"
		val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
		val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
		val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
		return if (hours > 0) {
			String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
		} else {
			String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
		}
	}

	fun formatFileSize(bytes: Long): String {
		if (bytes <= 0L) return "0 B"
		val units = arrayOf("B", "KB", "MB", "GB", "TB")
		var value = bytes.toDouble()
		var index = 0
		while (value >= 1024 && index < units.lastIndex) {
			value /= 1024
			index++
		}
		return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
	}

	fun formatSpeed(speed: Float): String =
		if (speed % 1f == 0f) "${speed.toInt()}x" else String.format(Locale.getDefault(), "%.2fx", speed)
}
