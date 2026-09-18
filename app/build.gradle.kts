plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.hilt.android)
	alias(libs.plugins.ksp)
}

import java.util.Properties

android {
	namespace = "com.puma.videomax"
	compileSdk {
		version = release(36) {
			minorApiLevel = 1
		}
	}

	defaultConfig {
		applicationId = "com.puma.videomax"
		minSdk = 26
		targetSdk = 36
		versionCode = 16
		versionName = "1.0.3"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		vectorDrawables {
			useSupportLibrary = true
		}
	}

	val uploadProps = Properties().apply {
		rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
	}
	val uploadStoreFile = uploadProps.getProperty("upload.storeFile")?.let { rootProject.file(it) }
		?.takeIf { it.exists() }

	signingConfigs {
		maybeCreate("release")
		getByName("release") {
			// Upload key loaded from local.properties (never committed).
			if (uploadStoreFile != null) {
				storeFile = uploadStoreFile
				storePassword = uploadProps.getProperty("upload.storePassword")
				keyAlias = uploadProps.getProperty("upload.keyAlias")
				keyPassword = uploadProps.getProperty("upload.keyPassword")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			isShrinkResources = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			if (uploadStoreFile != null) {
				signingConfig = signingConfigs.getByName("release")
			}
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.activity.compose)

	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.androidx.navigation.compose)

	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
	implementation(libs.androidx.hilt.navigation.compose)

	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	implementation(libs.androidx.room.paging)
	ksp(libs.androidx.room.compiler)

	implementation(libs.androidx.paging.runtime)
	implementation(libs.androidx.paging.compose)

	implementation(libs.androidx.datastore.preferences)

	implementation(libs.androidx.media)

	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.ui)
	implementation(libs.androidx.media3.session)
	implementation(libs.androidx.media3.common)
	implementation(libs.androidx.media3.decoder)
	implementation(libs.androidx.media3.exoplayer.dash)
	implementation(libs.androidx.media3.exoplayer.hls)

	implementation(libs.coil.compose)
	implementation(libs.coil.video)

	implementation(libs.play.services.ads)
	implementation(libs.play.review)
	implementation(libs.user.messaging.platform)
	implementation(libs.play.billing.ktx)
	// AdMob mediation (bidding + waterfall): Unity Ads + AppLovin.
	// Adapters self-initialize with MobileAds.initialize(); no manifest keys needed.
	implementation(libs.unity.ads)
	implementation(libs.mediation.unity)
	implementation(libs.mediation.applovin)

	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.kotlinx.coroutines.play.services)

	debugImplementation(libs.androidx.compose.ui.tooling)

	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}
