package com.example.sona.ui.navigation

enum class SonaDestination(
    val route: String,
    val label: String,
) {
    Library("library", "Library"),
    Downloads("downloads", "Downloads"),
    Playlists("playlists", "Playlists"),
    Settings("settings", "Settings"),
}
