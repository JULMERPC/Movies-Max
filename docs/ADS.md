# Google AdMob — Videomax (Jualix)

## Overview

Videomax integrates Google Mobile Ads SDK (Legacy) v25.4.0 for in-app advertising
and Google User Messaging Platform (UMP) SDK v4.0.0 for privacy/consent management.
The integration follows a centralized architecture that keeps all ad-related code
in a single `ads/` package, making it easy to add new formats without touching
existing screens.

## AdMob IDs

| Key | Test ID | Production ID |
|---|---|---|
| **Application ID** | `ca-app-pub-7120145882116895~3435821277` | (same) |
| **Banner** | `ca-app-pub-3940256099942544/6300978111` | `ca-app-pub-7120145882116895/6968706325` |
| **Rewarded** | `ca-app-pub-3940256099942544/5224354917` | `ca-app-pub-7120145882116895/9730601032` |

> The Application ID is configured in `AndroidManifest.xml` and must NOT change.

## Architecture

```
ads/
├── AdConfig.kt              — All ad unit IDs centralized (test + production)
├── AdsManager.kt            — Singleton manager: init, load, show for all formats
├── BannerAd.kt              — Compose wrapper for AdView (consent-gated)
├── ConsentManager.kt        — UMP consent management (singleton)
└── ConsentManagerProvider.kt — CompositionLocal for Compose access
```

### Key design decisions

- **AdConfig** is an `object` — no DI needed for reading IDs.
- **AdsManager** is a `@Singleton` injected via Hilt — handles interstitial,
  rewarded, native, and app open ads. Gates all ad loading behind consent.
- **ConsentManager** is a `@Singleton` injected via Hilt — manages UMP consent
  state, debug settings, and privacy options form.
- **BannerAd** is a pure Composable — uses `AndroidView` to wrap `AdView`.
  Only loads ads when `canRequestAds()` is true.
- Ads are **never** shown inside the player screen or over interactive elements.

## Consent & Privacy (UMP)

### Overview

Google User Messaging Platform (UMP) manages user consent for GDPR and other
privacy regulations. The consent form is created and published in AdMob.

### How it works

1. **App launch**: `MainActivity.onCreate()` calls
   `ConsentManager.requestConsentInfoUpdate()` to check if consent is required.
2. **Form display**: If consent is needed, `loadAndShowConsentFormIfRequired()`
   displays the official UMP consent form.
3. **Ad gating**: `AdsManager` checks `ConsentManager.canRequestAds()` before
   loading any ad. If consent is not granted, ad loading is deferred.
4. **Consent ready**: After consent is resolved, `AdsManager.onConsentReady()`
   triggers any pending ad loads.
5. **Privacy options**: In Settings, "Configuración de privacidad" appears only
   when required, allowing users to modify their consent at any time.

### Flow diagram

```
App Launch
  │
  ├── MobileAds.initialize()
  │
  └── ConsentManager.requestConsentInfoUpdate()
        │
        ├── Form needed? → loadAndShowConsentFormIfRequired()
        │                     │
        │                     ├── User consents → canRequestAds = true
        │                     │                    → Banner loads
        │                     │                    → Rewarded loads
        │                     │
        │                     └── User declines → canRequestAds = false
        │                                          → No ads loaded
        │
        └── No form needed → canRequestAds = true (from previous session)
                              → Banner loads
                              → Rewarded loads
```

### Key APIs

| Method | Description |
|---|---|
| `ConsentManager.requestConsentInfoUpdate(activity)` | Checks if consent is required |
| `ConsentManager.loadAndShowConsentFormIfRequired(activity)` | Shows consent form if needed |
| `ConsentManager.canRequestAds` | StateFlow<Boolean> — true when ads allowed |
| `ConsentManager.privacyOptionsRequired` | StateFlow<Boolean> — true if privacy menu needed |
| `ConsentManager.showPrivacyOptionsForm(activity)` | Opens privacy options form |
| `AdsManager.onConsentReady()` | Triggers pending ad loads after consent |

### Privacy Options in Settings

When `privacyOptionsRequirementStatus == REQUIRED`, a "Privacidad" section
appears in Settings with "Configuración de privacidad". Tapping it opens the
official UMP privacy options form.

### Layout

```
Ajustes
├── Apoya al desarrollador
├── Apariencia
├── Reproducción
├── Controles del reproductor
├── Archivos
├── Carpeta privada
├── Privacidad                          ← Only if required
│   └── Configuración de privacidad
└── Acerca de
```

## How to test

### Basic ad flow

1. Build and run on a device/emulator with Google Play Services.
2. **Banner**: Should display a Google test ad (blue "Ad" label) at the bottom.
3. **Rewarded**: Go to Ajustes > Apoya al desarrollador > tap "Ver anuncio".
   A test rewarded ad should appear. Completing it shows the thank-you message.

### Testing consent (EEA simulation)

1. Run the app in **DEBUG** build.
2. The device is automatically treated as being in the EEA.
3. On first launch, the UMP consent form should appear.
4. Test accepting, declining, and managing options.
5. After consent, verify `canRequestAds()` is true and ads load.
6. Go to Settings > Privacidad > Configuración de privacidad to modify.

### Getting your test device hashed ID

1. Run the app once in DEBUG mode.
2. Check Logcat for the `ConsentManager` tag.
3. Look for a message like:
   ```
   Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("XXXXXX")...
   ```
4. Copy the hashed ID if you need to add it manually.

### Resetting consent for testing

In DEBUG builds only, call `consentManager.reset()` to simulate a fresh install.
**Never call reset() in production.**

## Switching to production

1. In `AdConfig.kt`, set `USE_TEST_ADS = false`.
2. Verify the production ad unit IDs are filled in the `PRODUCTION_*` constants.
3. Verify the `<meta-data>` in `AndroidManifest.xml` still has the correct
   Application ID.
4. Remove any test device hashed IDs from the code.
5. The `DEBUG_GEOGRAPHY_EEA` setting is automatically disabled in RELEASE builds
   (guarded by `BuildConfig.DEBUG`).
6. Test thoroughly before publishing.

## Preloading

- **Banner**: Loads itself when the composable enters composition (consent-gated).
- **Rewarded**: Loaded when the SupportDeveloperScreen is opened.
  After consumption, it reloads automatically.

## Privacy & Google Play

### Advertising ID

The `com.google.android.gms.permission.AD_ID` permission is NOT currently
declared in the manifest. If your app targets users in regions where
consent is required (GDPR, CCPA), consider adding:

```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

and integrating the Google User Messaging Platform (UMP) SDK for consent flows.

### Data Safety declaration

In Google Play Console > App content > Data safety, declare:

- **Advertising ID is collected**: Yes (if you use personalized ads)
- **Purpose**: Advertising / remarketing
- **Data sharing**: With Google AdMob

### Test vs Production

| Setting | Value |
|---|---|
| `AdConfig.USE_TEST_ADS` | `true` during development |
| Test Ad Unit IDs | Google official test IDs |
| Production IDs | Filled in `PRODUCTION_*` constants |
| DEBUG EEA | Active only in DEBUG builds |
| Consent form | Published in AdMob |

## Troubleshooting

- **Banner not showing**: Check Logcat for `BannerAd` tag. Ensure device has
  internet and Google Play Services. Verify consent is granted.
- **Rewarded not loading**: Check Logcat for `AdsManager` tag. The button
  shows "Preparando anuncio..." while loading, and "Reintentar" on failure.
  Verify consent is granted.
- **Consent form not appearing**: Check Logcat for `ConsentManager` tag.
  Ensure the consent message is published in AdMob.
- **Privacy options not showing**: The option only appears when
  `privacyOptionsRequirementStatus == REQUIRED`. Check Logcat for consent
  status.
- **Build error**: Run `./gradlew clean assembleDebug`. Verify
  `play-services-ads` and `user-messaging-platform` dependencies resolved.
- **Crash on MobileAds.init**: Ensure `<meta-data>` Application ID matches
  exactly.
