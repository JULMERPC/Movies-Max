package com.puma.videomax

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.puma.videomax.ads.AdConfig
import com.puma.videomax.ads.AppOpenAdManager
import com.puma.videomax.ads.MediationLogger
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

private const val TAG = "VideoPlayerProApp"

@HiltAndroidApp
class VideoPlayerProApp : Application(), ImageLoaderFactory {

	@Inject
	lateinit var appOpenAdManager: AppOpenAdManager

	override fun onCreate() {
		super.onCreate()
		AdConfig.init(this)
		Log.d(TAG, "MobileAds.initialize starting")
		MobileAds.initialize(this) { initializationStatus ->
			Log.d(TAG, "MobileAds.initialize completed")
			MediationLogger.logInitStatus(initializationStatus.adapterStatusMap)
			appOpenAdManager.register()
		}
	}

	override fun newImageLoader(): ImageLoader =
		ImageLoader.Builder(this)
			.components { add(VideoFrameDecoder.Factory()) }
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
					.maxSizeBytes(100L * 1024 * 1024)
					.build()
			}
			.crossfade(false)
			.build()
}
