package com.example.sona.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song

@Composable
fun PlaylistsScreen(
    contentPadding: PaddingValues,
    uiState: PlaylistsUiState,
    onNameChange: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onBackToPlaylists: () -> Unit,
    onAddSong: (Song) -> Unit,
    onRemoveSong: (Song) -> Unit,
    onPlaySongs: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.selectedPlaylist != null) {
        PlaylistDetail(
            contentPadding = contentPadding,
            uiState = uiState,
            onBack = onBackToPlaylists,
            onAddSong = onAddSong,
            onRemoveSong = onRemoveSong,
            onPlaySongs = onPlaySongs,
            modifier = modifier,
        )
    } else {
        PlaylistList(
            contentPadding = contentPadding,
            uiState = uiState,
            onNameChange = onNameChange,
            onCreatePlaylist = onCreatePlaylist,
            onSelectPlaylist = onSelectPlaylist,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaylistList(
    contentPadding: PaddingValues,
    uiState: PlaylistsUiState,
    onNameChange: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Playlists",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.newPlaylistName,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                label = { Text(text = "New playlist") },
                singleLine = true,
            )
            Button(onClick = onCreatePlaylist) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Create")
            }
        }

        if (uiState.playlists.isEmpty()) {
            Text(
                text = "Create a playlist to group songs from your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = uiState.playlists,
                    key = { playlist -> playlist.id },
                ) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onSelectPlaylist(playlist) },
                        headlineContent = {
                            Text(
                                text = playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(text = "Tap to edit")
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    contentPadding: PaddingValues,
    uiState: PlaylistsUiState,
    onBack: () -> Unit,
    onAddSong: (Song) -> Unit,
    onRemoveSong: (Song) -> Unit,
    onPlaySongs: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPlaylist = uiState.selectedPlaylist ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = selectedPlaylist.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = { onPlaySongs(uiState.playlistSongs) },
                enabled = uiState.playlistSongs.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play playlist",
                )
            }
        }

        Text(
            text = "Playlist songs",
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.playlistSongs.isEmpty()) {
            Text(
                text = "Add songs from your library below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SongList(
                songs = uiState.playlistSongs,
                modifier = Modifier.weight(1f),
                actionIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                    )
                },
                onAction = onRemoveSong,
            )
        }

        Text(
            text = "Add from library",
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.availableSongs.isEmpty()) {
            Text(
                text = "Every library song is already in this playlist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SongList(
                songs = uiState.availableSongs,
                modifier = Modifier.weight(1f),
                actionIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                onAction = onAddSong,
            )
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    actionIcon: @Composable () -> Unit,
    onAction: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(
            items = songs,
            key = { song -> song.id },
        ) { song ->
            ListItem(
                headlineContent = {
                    Text(
                        text = song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        text = song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onAction(song) }) {
                        actionIcon()
                    }
                },
            )
            HorizontalDivider()
        }
    }
}
