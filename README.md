<p align="center">
  <img src="app/src/main/res/drawable/sona_logo.png" width="128" height="128" alt="Sona Logo">
</p>

<h1 align="center">Sona</h1>

<p align="center">
  A modern, elegant Android music player built with Kotlin, Jetpack Compose, and Media3.
</p>

## Download

[Download the latest MVP APK from GitHub Releases](https://github.com/lucifron28/Sona/releases)

Local release builds are generated at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Features

* 🎵 **Local Library Management:** Seamlessly browse and manage your local audio files.
* 🎧 **Modern Playback Engine:** Powered by AndroidX Media3 (ExoPlayer) for stable, gapless, and background audio playback.
* 📥 **Audio Downloader:** Integrated with `youtube-dl` to easily download and import audio from URLs directly into your library.
* 🎨 **Dynamic UI & Theming:** 
  * Fully built with Jetpack Compose.
  * Material 3 Dynamic Colors support.
  * Light, Dark, and custom 'Dracula' themes.
  * Custom rotating vinyl record animations for tracks without album art.
* 📂 **Playlists:** Create and manage custom playlists.
* 📱 **Mini & Expanded Player:** Fluid transitions between a persistent mini-player and a fully featured now-playing screen.

## Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Media Playback:** [AndroidX Media3](https://developer.android.com/guide/topics/media/media3)
* **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
* **Navigation:** Compose Navigation
* **Audio Downloading:** [youtubedl-android](https://github.com/yausername/youtubedl-android)

## Getting Started

### Prerequisites
* Android Studio (latest stable or preview version recommended)
* JDK 17+
* Android SDK API Level 36 (Android 15+)

### Build & Run
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the `app` configuration on an emulator or physical device.
