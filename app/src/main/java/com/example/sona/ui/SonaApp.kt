package com.example.sona.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.sona.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sona.di.AppContainer
import com.example.sona.ui.downloads.DownloadsScreen
import com.example.sona.ui.downloads.DownloadsViewModel
import com.example.sona.ui.downloads.DownloadsViewModelFactory
import com.example.sona.ui.library.LibraryScreen
import com.example.sona.ui.library.LibraryViewModel
import com.example.sona.ui.library.LibraryViewModelFactory
import com.example.sona.ui.navigation.SonaDestination
import com.example.sona.ui.nowplaying.MiniPlayer
import com.example.sona.ui.nowplaying.NowPlayingScreen
import com.example.sona.ui.playlists.PlaylistsScreen
import com.example.sona.ui.playlists.PlaylistsViewModel
import com.example.sona.ui.playlists.PlaylistsViewModelFactory
import com.example.sona.ui.settings.SettingsScreen
import com.example.sona.ui.settings.SettingsViewModel
import com.example.sona.ui.settings.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonaApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val playbackState by appContainer.playerController.playbackState.collectAsStateWithLifecycle()
    var showNowPlayingSheet by remember { mutableStateOf(false) }
    val nowPlayingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val libraryViewModelFactory = remember(appContainer) {
        LibraryViewModelFactory(
            songRepository = appContainer.songRepository,
            playlistRepository = appContainer.playlistRepository,
            appMusicStorage = appContainer.appMusicStorage,
        )
    }
    val settingsViewModelFactory = remember(appContainer) {
        SettingsViewModelFactory(appContainer.settingsRepository)
    }
    val playlistsViewModelFactory = remember(appContainer) {
        PlaylistsViewModelFactory(
            playlistRepository = appContainer.playlistRepository,
            songRepository = appContainer.songRepository,
        )
    }
    val downloadsViewModelFactory = remember(appContainer) {
        DownloadsViewModelFactory(
            downloadRepository = appContainer.downloadRepository,
            urlImportManager = appContainer.urlImportManager,
        )
    }
    val currentSongId = playbackState.currentSong?.id

    LaunchedEffect(currentSongId) {
        currentSongId?.let { songId ->
            appContainer.songRepository.recordPlayed(songId, System.currentTimeMillis())
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.sona_logo),
                        contentDescription = "Sona",
                        modifier = Modifier.height(36.dp),
                    )
                },
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                MiniPlayer(
                    playbackState = playbackState,
                    onExpand = { showNowPlayingSheet = true },
                    onPlayPause = appContainer.playerController::playPause,
                )
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SonaDestination.Library.route,
            modifier = Modifier,
        ) {
            composable(SonaDestination.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = libraryViewModelFactory,
                )
                val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

                LibraryScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onImportAudio = libraryViewModel::importAudio,
                    onSongClick = { song, queue ->
                        appContainer.playerController.play(song, queue)
                    },
                    onViewSelected = libraryViewModel::setView,
                    onFilterSelected = libraryViewModel::setFilter,
                    onSearchQueryChange = libraryViewModel::setSearchQuery,
                    onFavoriteClick = libraryViewModel::toggleFavorite,
                    onSongLongClick = libraryViewModel::showTrackActions,
                    onRenameFromActions = libraryViewModel::renameFromTrackActions,
                    onAddToPlaylist = libraryViewModel::addTrackActionSongToPlaylist,
                    onDismissTrackActions = libraryViewModel::dismissTrackActions,
                    onEditTitleChange = libraryViewModel::updateEditTitle,
                    onEditArtistChange = libraryViewModel::updateEditArtist,
                    onSaveEdit = libraryViewModel::saveEditedTrack,
                    onDismissEdit = libraryViewModel::dismissEditor,
                    onClearError = libraryViewModel::clearError,
                )
            }
            composable(SonaDestination.Downloads.route) {
                val downloadsViewModel: DownloadsViewModel = viewModel(
                    factory = downloadsViewModelFactory,
                )
                val uiState by downloadsViewModel.uiState.collectAsStateWithLifecycle()

                DownloadsScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onUrlChange = downloadsViewModel::setUrl,
                    onImportClick = downloadsViewModel::enqueueImport,
                    onUpdateDownloader = downloadsViewModel::updateDownloader,
                    onDeleteDownload = downloadsViewModel::deleteDownload,
                    onSnackbarShown = downloadsViewModel::consumeSnackbar,
                )
            }
            composable(SonaDestination.Playlists.route) {
                val playlistsViewModel: PlaylistsViewModel = viewModel(
                    factory = playlistsViewModelFactory,
                )
                val uiState by playlistsViewModel.uiState.collectAsStateWithLifecycle()

                PlaylistsScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onNameChange = playlistsViewModel::setNewPlaylistName,
                    onCreatePlaylist = playlistsViewModel::createPlaylist,
                    onSelectPlaylist = playlistsViewModel::selectPlaylist,
                    onBackToPlaylists = playlistsViewModel::clearSelectedPlaylist,
                    onAddSong = playlistsViewModel::addSong,
                    onRemoveSong = playlistsViewModel::removeSong,
                    onPlaySongs = { songs ->
                        songs.firstOrNull()?.let { firstSong ->
                            appContainer.playerController.play(firstSong, songs)
                        }
                    },
                )
            }
            composable(SonaDestination.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = settingsViewModelFactory,
                )
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onThemeSelected = settingsViewModel::setThemePreference,
                )
            }
        }
    }

    if (showNowPlayingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNowPlayingSheet = false },
            sheetState = nowPlayingSheetState,
        ) {
            NowPlayingScreen(
                contentPadding = PaddingValues(bottom = 24.dp),
                playbackState = playbackState,
                onPlayPause = appContainer.playerController::playPause,
                onSeek = appContainer.playerController::seekTo,
                onSkipNext = appContainer.playerController::skipNext,
                onSkipPrevious = appContainer.playerController::skipPrevious,
            )
        }
    }
}

private val SonaDestination.icon: ImageVector
    get() = when (this) {
        SonaDestination.Library -> Icons.Filled.LibraryMusic
        SonaDestination.Downloads -> Icons.Filled.Download
        SonaDestination.Playlists -> Icons.AutoMirrored.Filled.QueueMusic
        SonaDestination.Settings -> Icons.Filled.Settings
    }

private val NavDestinationLabel = mapOf(
    SonaDestination.Library.route to SonaDestination.Library.label,
    SonaDestination.Downloads.route to SonaDestination.Downloads.label,
    SonaDestination.Playlists.route to SonaDestination.Playlists.label,
    SonaDestination.Settings.route to SonaDestination.Settings.label,
)

private val sonaDestinations = listOf(
    SonaDestination.Library,
    SonaDestination.Downloads,
    SonaDestination.Playlists,
    SonaDestination.Settings,
)
