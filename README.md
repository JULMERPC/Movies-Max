# videomax

Professional Android video player built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **MVVM + Clean Architecture**, **Hilt**, **Room**, **DataStore**, and **Media3 (ExoPlayer)**.

## Features

- Automatic MediaStore scan of device videos
- Library with search, folder filters, and multiple sort options
- Grid / list layouts with lazy loading and Coil thumbnail cache
- Full player controls: play/pause, seek, speed, resize, lock
- Gesture brightness (left) and volume (right), horizontal scrub
- Double-tap seek, Picture-in-Picture, keep-screen-on
- External subtitles: SRT, VTT, ASS/SSA (sidecar + file picker)
- Favorites, playback history with resume, playlists
- Video details: resolution, duration, size, codec, path
- Light / Dark / System themes via DataStore

## Architecture

```
presentation/   Compose UI + ViewModels
domain/         Models, repository contracts, use cases
data/           MediaStore, Room, DataStore, repository impls
di/             Hilt modules
```

## Requirements

- Android Studio Otter / recent stable with AGP 9.3+
- JDK 17
- minSdk 26, targetSdk 36

## Build

```bash
./gradlew :app:assembleDebug
```

Open the project in Android Studio and run on a device/emulator with local videos.

## Permissions

- `READ_MEDIA_VIDEO` (API 33+)
- `READ_EXTERNAL_STORAGE` (API ≤ 32)
