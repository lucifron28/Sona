package com.example.sona.ui.navigation

enum class SonaDestination(
    val route: String,
    val label: String,
) {
    Library("library", "Library"),
    NowPlaying("now_playing", "Now Playing"),
    Playlists("playlists", "Playlists"),
    Settings("settings", "Settings"),
}
