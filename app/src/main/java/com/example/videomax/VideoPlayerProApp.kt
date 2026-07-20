package com.example.videomax

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoPlayerProApp : Application(), ImageLoaderFactory {

	override fun newImageLoader(): ImageLoader =
		ImageLoader.Builder(this)
			.components { add(VideoFrameDecoder.Factory()) }
			.memoryCache {
				MemoryCache.Builder(this)
					.maxSizePercent(0.25)
					.build()
			}
			.diskCache {
				DiskCache.Builder()
					.directory(cacheDir.resolve("video_thumbs"))
					.maxSizeBytes(250L * 1024 * 1024)
					.build()
			}
			.crossfade(true)
			.build()
}
