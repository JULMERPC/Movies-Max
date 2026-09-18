package com.puma.videomax.ads

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.puma.videomax.presentation.theme.VideoMaxDimens

private const val TAG = "NativeAdCard"

/**
 * A native ad slot for lazy lists (see [com.puma.videomax.domain.ads.NativeAdPlacer]).
 * Renders nothing while loading, on failure, or when ads are removed —
 * the surrounding list simply collapses the gap.
 */
@Composable
fun NativeAdSlot(
	modifier: Modifier = Modifier,
	viewModel: NativeAdViewModel = hiltViewModel()
) {
	val visible by viewModel.visible.collectAsStateWithLifecycle()
	if (!visible) return

	var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

	LaunchedEffect(viewModel) {
		nativeAd = runCatching { viewModel.loadAd() }.getOrNull()
	}

	// Validación de carga: el slot solo existe cuando el anuncio cargó al
	// 100%. Cargando o fallido → sin composable (la lista colapsa el hueco,
	// sin espacios en blanco ni vistas que no responden a clics).
	val ad = nativeAd ?: return

	// El DisposableEffect captura el ad RESUELTO (el mismo valor de la key),
	// nunca la variable de estado `nativeAd`: onDispose corre DESPUÉS del
	// cambio de estado, así que leer `nativeAd` ahí destruía el anuncio recién
	// cargado en la transición null → ad. Un NativeAd destruido devuelve assets
	// vacíos y clics muertos: exactamente la "caja blanca sin nada" en la lista.
	DisposableEffect(ad) {
		onDispose {
			ad.destroy()
			Log.d(TAG, "NativeAd destroyed")
		}
	}

	Card(
		modifier = modifier
			.fillMaxWidth()
			.wrapContentHeight()
			.padding(
				horizontal = VideoMaxDimens.spacingMd,
				vertical = VideoMaxDimens.spacingSm
			),
		shape = MaterialTheme.shapes.medium,
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
		)
	) {
		// AUDIT-CLICK: key(ad) ties the view to exactly one NativeAd object.
		// Before, factory() captured the first ad forever: any reload path
		// left a stale/destroyed ad on screen whose taps did nothing.
		// update{} rebinds so clicks always hit the live ad object.
		key(ad) {
			AndroidView(
				factory = { ctx -> buildNativeAdView(ctx, ad) },
				update = { view -> view.setNativeAd(ad) },
				// Adaptable (wrap_content): sin height fijo ni clip, para que
				// Headline + MediaView + Body + CTA rendericen completos y
				// reciban clics. Un height fijo recortaba el anuncio a la mitad.
				modifier = Modifier.fillMaxWidth().wrapContentHeight()
			)
		}
	}
}

@Suppress("SetTextI18n")
private fun buildNativeAdView(
	context: android.content.Context,
	ad: NativeAd
): NativeAdView {
	Log.d(TAG, "bind ad: headline=${ad.headline != null} media=${ad.mediaContent != null} " +
		"icon=${ad.icon != null} body=${ad.body != null} cta=${ad.callToAction != null} " +
		"advertiser=${ad.advertiser != null}")
	val density = context.resources.displayMetrics.density
	fun dp(value: Int): Int = (value * density).toInt()

	val adView = NativeAdView(context).apply {
		layoutParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.WRAP_CONTENT
		)
	}

	val container = LinearLayout(context).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(dp(12), dp(12), dp(12), dp(12))
	}

	val badge = TextView(context).apply {
		text = "Anuncio"
		setTextColor(0xFFFFFFFF.toInt())
		textSize = 11f
		setBackgroundColor(0xFF757575.toInt())
		setPadding(dp(8), dp(2), dp(8), dp(2))
	}
	container.addView(badge)

	val headline = TextView(context).apply {
		text = ad.headline
		textSize = 16f
		setTypeface(typeface, android.graphics.Typeface.BOLD)
		setPadding(0, dp(8), 0, 0)
	}
	adView.headlineView = headline
	container.addView(headline)

	if (ad.mediaContent != null) {
		val media = MediaView(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(176)
			).apply { topMargin = dp(8) }
			mediaContent = ad.mediaContent
		}
		adView.mediaView = media
		container.addView(media)
	} else {
		ad.icon?.drawable?.let { drawable ->
			val icon = ImageView(context).apply {
				layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
					.apply { topMargin = dp(8) }
				setImageDrawable(drawable)
			}
			adView.iconView = icon
			container.addView(icon)
		}
	}

	ad.body?.let { bodyText ->
		val body = TextView(context).apply {
			text = bodyText
			textSize = 14f
			setPadding(0, dp(8), 0, 0)
		}
		adView.bodyView = body
		container.addView(body)
	}

	val footer = LinearLayout(context).apply {
		orientation = LinearLayout.HORIZONTAL
		setPadding(0, dp(8), 0, 0)
	}

	ad.advertiser?.let { advertiserText ->
		val advertiser = TextView(context).apply {
			text = advertiserText
			textSize = 12f
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}
		adView.advertiserView = advertiser
		footer.addView(advertiser)
	}

	ad.callToAction?.let { ctaText ->
		val cta = Button(context).apply {
			text = ctaText
			visibility = View.VISIBLE
		}
		adView.callToActionView = cta
		footer.addView(cta)
	}
	container.addView(footer)

	adView.addView(container)
	adView.setNativeAd(ad)
	return adView
}
