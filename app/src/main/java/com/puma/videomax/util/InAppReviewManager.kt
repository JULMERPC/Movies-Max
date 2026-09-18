package com.puma.videomax.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.puma.videomax.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewManager @Inject constructor(
	private val settingsDataStore: SettingsDataStore
) {

	companion object {
		private const val TAG = "InAppReviewManager"
		private const val MIN_PLAYBACKS_FOR_REVIEW = 5
		private val COOLDOWN_PERIOD = TimeUnit.DAYS.toMillis(7)
	}

	suspend fun onVideoPlaybackCompleted(durationMs: Long) {
		if (durationMs < TimeUnit.MINUTES.toMillis(1)) return

		settingsDataStore.incrementVideoPlaybackCount()

		if (shouldRequestReview()) {
			settingsDataStore.setLastReviewPromptTimestamp(System.currentTimeMillis())
		}
	}

	suspend fun tryLaunchReviewFlow(activity: Activity) {
		if (!shouldRequestReview()) return

		try {
			val manager = ReviewManagerFactory.create(activity)
			val reviewInfo = manager.requestReviewFlow().await()
			val flow = manager.launchReviewFlow(activity, reviewInfo)
			flow.await()

			settingsDataStore.setHasRatedOrDismissed(true)
			Log.d(TAG, "Review flow completed")
		} catch (e: Exception) {
			Log.w(TAG, "Review flow failed or cancelled", e)
		}
	}

	private suspend fun shouldRequestReview(): Boolean {
		val playbackCount = settingsDataStore.getVideoPlaybackCount()
		val hasRatedOrDismissed = settingsDataStore.getHasRatedOrDismissed()
		val lastPromptTimestamp = settingsDataStore.getLastReviewPromptTimestamp()
		val cooldownElapsed = System.currentTimeMillis() - lastPromptTimestamp >= COOLDOWN_PERIOD

		return playbackCount >= MIN_PLAYBACKS_FOR_REVIEW &&
			!hasRatedOrDismissed &&
			cooldownElapsed
	}
}
