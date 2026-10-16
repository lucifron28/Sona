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

The latest signed release is available from [GitHub Releases](https://github.com/lucifron28/Sona/releases/latest).

Release builds include ABI-specific APKs and a universal APK. Choose the matching device architecture for a smaller download, or use the universal APK for broader compatibility.

Android may ask you to allow installs from your browser or file manager because these APKs are sideloaded.

## Why Sona

Sona is a local-first music player for Android, built with Compose, Media3, Room, and yt-dlp powered audio imports.

Import local audio or authorized URLs, edit metadata, organize playback, and listen through the mini player and expanded now-playing view.

## Features

- Local audio import through the Android file picker without broad storage permissions; embedded artwork is extracted when available.
- Room-backed library persistence for songs, metadata, playlists, and download records, with migrations from every supported database version.
- Media3 playback with queue, shuffle, repeat, seek, next, and previous controls; queue and position restore after process restart.
- Audio-focus handling and pause-on-headphone-disconnect behavior.
- Searchable library with Songs, Artists, and Albums views, distinct empty states, and album grouping by title and artist.
- Artist and album drill-down views that open like folders before playback.
- Visible track overflow actions, long-press actions, and accessible queue move controls.
- URL audio import powered by yt-dlp through youtubedl-android.
- Network-aware, resumable URL imports with bounded transient retries, serialized yt-dlp operations, cancellation, retry, and completion actions.
- Download stages, foreground progress notification, throttled progress persistence, and detailed diagnostics behind a details action.
- Android share-to-Sona support for shared text URLs.
- One stable yt-dlp update channel shared by manual and automatic updates.
- Light, dark, system, and Dracula theme options.
- Release builds use R8/resource shrinking and include ABI-specific and universal APKs.

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
4. Build the release APKs:

```powershell
.\gradlew.bat :app:assembleRelease
```

Release APKs are generated under `app/build/outputs/apk/release/`. The universal APK supports every ABI included in the build; install an ABI-specific APK for a smaller package.

Keep your release keystore backed up. Android updates must be signed with the same key.

## Roadmap

- MediaStore scanning for local device music.
- Lyrics support.
- Equalizer and audio effects.
- Smarter playlist management.
- Better first-run onboarding.
- Screenshots and demo video in the README.

## Contributing

Feedback, bug reports, and focused pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the preferred workflow.

## License

Sona is licensed under the [Apache License 2.0](LICENSE).
