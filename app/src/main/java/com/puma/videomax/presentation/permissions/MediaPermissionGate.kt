package com.puma.videomax.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.presentation.theme.VideoMaxTheme

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
				.padding(VideoMaxDimens.spacingXxxl),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(
				imageVector = Icons.Default.VideoLibrary,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(64.dp)
			)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingLg))
			Text(
				text = "Accedé a tu contenido multimedia",
				style = MaterialTheme.typography.headlineMedium,
				color = VideoMaxTheme.extended.textPrimary
			)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingSm))
			Text(
				text = "videomax necesita permiso para escanear y reproducir los videos y música almacenados en este dispositivo.",
				style = MaterialTheme.typography.bodyLarge,
				color = VideoMaxTheme.extended.textTertiary,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(VideoMaxDimens.spacingXxl))
			Button(onClick = onRequestPermission) {
				Text("Conceder acceso")
			}
		}
	}
}

fun requiredMediaPermissions(): Array<String> =
	when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
			arrayOf(
				Manifest.permission.READ_MEDIA_VIDEO,
				Manifest.permission.READ_MEDIA_AUDIO,
				Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
			)
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
			arrayOf(
				Manifest.permission.READ_MEDIA_VIDEO,
				Manifest.permission.READ_MEDIA_AUDIO
			)
		else ->
			arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
	}

fun hasMediaPermission(activity: Activity): Boolean {
	val selected = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	val video = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_MEDIA_VIDEO
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	val audio = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_MEDIA_AUDIO
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	val storage = ContextCompat.checkSelfPermission(
		activity, Manifest.permission.READ_EXTERNAL_STORAGE
	) == android.content.pm.PackageManager.PERMISSION_GRANTED
	return selected || video || audio || storage
}
