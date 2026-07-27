package com.example.videomax.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

@Composable
fun MediaPermissionGate(
	hasPermission: Boolean,
	onRequestPermission: () -> Unit,
	content: @Composable () -> Unit
) {
	if (hasPermission) {
		content()
	} else {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(32.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(
				imageVector = Icons.Default.VideoLibrary,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary
			)
			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = "Access your videos",
				style = MaterialTheme.typography.headlineMedium
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = "videomax needs permission to scan and play videos stored on this device.",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(24.dp))
			Button(onClick = onRequestPermission) {
				Text("Conceder acceso")
			}
		}
	}
}

/**
 * Returns the appropriate media permission string based on API level.
 * - API 34+ (Android 14+): READ_MEDIA_VISUAL_USER_SELECTED (partial access)
 * - API 33 (Android 13): READ_MEDIA_VIDEO
 * - API 32 and below: READ_EXTERNAL_STORAGE
 */
fun requiredMediaPermission(): String =
	when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
			Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
			Manifest.permission.READ_MEDIA_VIDEO
		else ->
			Manifest.permission.READ_EXTERNAL_STORAGE
	}

/**
 * Checks if the app has sufficient media access permission.
 * On Android 14+, READ_MEDIA_VISUAL_USER_SELECTED grants partial access.
 * Also checks READ_MEDIA_VIDEO since it may be co-granted for full access.
 */
fun hasMediaPermission(activity: Activity): Boolean {
	val selected = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	val video = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_MEDIA_VIDEO
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	val storage = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_EXTERNAL_STORAGE
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	return selected || video || storage
}
