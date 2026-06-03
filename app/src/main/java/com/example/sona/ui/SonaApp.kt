package com.example.sona.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sona.ui.library.LibraryScreen
import com.example.sona.ui.navigation.SonaDestination
import com.example.sona.ui.nowplaying.NowPlayingScreen
import com.example.sona.ui.playlists.PlaylistsScreen
import com.example.sona.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonaApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentDestination
                            ?.route
                            ?.let(NavDestinationLabel::get)
                            ?: SonaDestination.Library.label,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                sonaDestinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(text = destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SonaDestination.Library.route,
            modifier = Modifier,
        ) {
            composable(SonaDestination.Library.route) {
                LibraryScreen(contentPadding = innerPadding)
            }
            composable(SonaDestination.NowPlaying.route) {
                NowPlayingScreen(contentPadding = innerPadding)
            }
            composable(SonaDestination.Playlists.route) {
                PlaylistsScreen(contentPadding = innerPadding)
            }
            composable(SonaDestination.Settings.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
        }
    }
}

private val SonaDestination.icon: ImageVector
    get() = when (this) {
        SonaDestination.Library -> Icons.Filled.LibraryMusic
        SonaDestination.NowPlaying -> Icons.Filled.MusicNote
        SonaDestination.Playlists -> Icons.AutoMirrored.Filled.QueueMusic
        SonaDestination.Settings -> Icons.Filled.Settings
    }

private val NavDestinationLabel = mapOf(
    SonaDestination.Library.route to SonaDestination.Library.label,
    SonaDestination.NowPlaying.route to SonaDestination.NowPlaying.label,
    SonaDestination.Playlists.route to SonaDestination.Playlists.label,
    SonaDestination.Settings.route to SonaDestination.Settings.label,
)

private val sonaDestinations = listOf(
    SonaDestination.Library,
    SonaDestination.NowPlaying,
    SonaDestination.Playlists,
    SonaDestination.Settings,
)
