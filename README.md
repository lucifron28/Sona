<p align="center">
  <img src="app/src/main/res/drawable/sona_logo.png" width="128" height="128" alt="Sona logo">
</p>

<h1 align="center">Sona</h1>

<p align="center">
  A modern Android music player built with Kotlin, Jetpack Compose, Media3, Room, and yt-dlp powered audio imports.
</p>

<p align="center">
  <a href="https://github.com/lucifron28/Sona/releases/latest">Download APK</a>
  |
  <a href="#features">Features</a>
  |
  <a href="#roadmap">Roadmap</a>
  |
  <a href="#build-from-source">Build</a>
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue">
</p>

## Download

The latest signed MVP build is available from GitHub Releases:

[Download Sona v0.1.2](https://github.com/lucifron28/Sona/releases/tag/v0.1.2)

Android may ask you to allow installs from your browser or file manager because this is a sideloaded APK.

## Why Sona

Sona is a focused local-first music player for Android. It combines a clean Compose interface, reliable Media3 playback, local library management, and direct URL audio imports into one lightweight MVP.

The project is still early, but the core flow is usable: import or download a track, edit metadata, organize playback, and listen through a Spotify-style mini player and expanded now-playing view.

## Features

- Local audio import through the Android file picker without broad storage permissions.
- Room-backed library persistence for songs, metadata, playlists, and download records.
- Media3 ExoPlayer playback with queue, shuffle, repeat, seek, next, and previous controls.
- Spotify-inspired mini player that expands into a full now-playing screen.
- Searchable library with Songs, Artists, and Albums views.
- Artist and album drill-down views that open like folders before playback.
- Long-press track actions for rename, favorite, and playlist actions.
- Editable track name and artist, including existing artist suggestions.
- URL audio import powered by youtubedl-android and yt-dlp.
- Faster YouTube imports by avoiding duplicate metadata extraction.
- Download progress, diagnostics, success and failure snackbar feedback.
- Light, dark, system, and Dracula theme options.
- Signed release APK builds for direct Android installation.

## Screenshots

Screenshots and a short demo GIF are planned for the next README pass. Recommended shots:

- Library with Songs, Artists, and Albums.
- URL download screen with progress.
- Mini player and expanded now-playing screen.
- Queue reorder and swipe-to-delete interaction.
- Dracula theme.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3 and ExoPlayer
- Room
- WorkManager
- Navigation Compose
- youtubedl-android and yt-dlp

## Build From Source

### Requirements

- Android Studio
- JDK 17 or newer
- Android SDK API 36

### Debug Build

```powershell
.\gradlew.bat :app:assembleDebug
```

### Unit Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

### Signed Release Build

Release signing uses an ignored root-level `keystore.properties` file.

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Point `storeFile` to your release keystore.
3. Fill in the store and key passwords.
4. Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Signed local release builds are generated at:

```text
app/build/outputs/apk/release/app-release.apk
```

Keep your release keystore backed up. Android updates must be signed with the same key.

## Roadmap

- Background playback service and notification controls.
- MediaStore scanning for local device music.
- Album artwork extraction and editing.
- Lyrics support.
- Equalizer and audio effects.
- Smarter playlist management.
- Download queue controls and cancellation.
- Better first-run onboarding.
- Screenshots and demo video in the README.

## Contributing

Feedback, bug reports, and focused pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the preferred workflow.

## License

Sona is licensed under the [Apache License 2.0](LICENSE).
