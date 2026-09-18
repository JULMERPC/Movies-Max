package com.puma.videomax.service.audio

import androidx.media3.common.PlaybackException

/**
 * Codec tolerance policy (Namida: `InternalPlayerType.auto` + MPV fallback).
 *
 * Native `MediaCodec` has no decoder for Monkey's Audio (`.ape`) on virtually
 * all devices. Instead of killing the service, skip the track gracefully and
 * leave room for an FFmpeg extension renderer (`media3-decoder-ffmpeg`).
 */
object CodecFallbackPolicy {

	fun isMissingCodec(e: PlaybackException): Boolean =
		e.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
			e.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
			e.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
			(e.errorCodeName?.contains("Decoder", ignoreCase = true) == true)

	fun isUnsupportedApe(mime: String?): Boolean =
		mime.equals("audio/ape", ignoreCase = true) ||
			mime.equals("audio/x-ape", ignoreCase = true) ||
			mime.equals("audio/monkeys-audio", ignoreCase = true)

	fun shouldSkip(mime: String?, e: PlaybackException): Boolean =
		isMissingCodec(e) || isUnsupportedApe(mime)
}
