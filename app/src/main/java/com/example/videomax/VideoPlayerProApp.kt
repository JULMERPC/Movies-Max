package com.example.videomax

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers

@HiltAndroidApp
class VideoPlayerProApp : Application(), ImageLoaderFactory {

	override fun newImageLoader(): ImageLoader =
		ImageLoader.Builder(this)
			.components { add(VideoFrameDecoder.Factory()) }
			// Limit parallel video-frame decodes — too many MediaCodec jobs freeze the UI.
			.decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
			.fetcherDispatcher(Dispatchers.IO.limitedParallelism(4))
			.memoryCache {
				MemoryCache.Builder(this)
					.maxSizePercent(0.20)
					.build()
			}
			.diskCache {
				DiskCache.Builder()
					.directory(cacheDir.resolve("video_thumbs"))
					.maxSizeBytes(200L * 1024 * 1024)
					.build()
			}
			.crossfade(false)
			.build()
}
