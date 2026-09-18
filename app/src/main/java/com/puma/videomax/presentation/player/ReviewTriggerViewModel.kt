package com.puma.videomax.presentation.player

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.puma.videomax.util.InAppReviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReviewTriggerViewModel @Inject constructor(
	val inAppReviewManager: InAppReviewManager
) : ViewModel() {
	suspend fun tryLaunchReview(activity: Activity) {
		inAppReviewManager.tryLaunchReviewFlow(activity)
	}
}
