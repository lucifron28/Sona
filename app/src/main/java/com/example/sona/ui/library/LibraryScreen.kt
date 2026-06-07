package com.example.sona.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sona.core.utils.formatDuration
import com.example.sona.domain.model.Playlist
import com.example.sona.domain.model.Song

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    uiState: LibraryUiState,
    onImportAudio: (android.net.Uri) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onViewSelected: (LibraryView) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onGroupClick: (LibraryView, LibraryGroup) -> Unit,
    onBackFromGroup: () -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onRenameFromActions: (Song) -> Unit,
    onDeleteFromActions: (Song) -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onDismissTrackActions: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditArtistChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onDismissEdit: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onImportAudio)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LibraryHeader(
            isImporting = uiState.isImporting,
            onImportClick = {
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
        )
        LibraryViewTabs(
            selectedView = uiState.selectedView,
            onViewSelected = onViewSelected,
        )
        LibrarySearchField(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        LibraryFilters(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
        )

        if (uiState.isImporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.errorMessage?.let { errorMessage ->
            ImportError(
                message = errorMessage,
                onClearError = onClearError,
            )
        }

        val selectedGroup = uiState.selectedGroup
        when {
            selectedGroup != null -> {
                LibraryGroupDetail(
                    selectedGroup = selectedGroup,
                    onBack = onBackFromGroup,
                    onSongClick = onSongClick,
                    onSongLongClick = onSongLongClick,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.weight(1f),
                )
            }
            uiState.isSelectedViewEmpty -> {
                EmptyLibrary(modifier = Modifier.weight(1f))
            }
            else -> {
                LibraryContent(
                    uiState = uiState,
                    onSongClick = onSongClick,
                    onGroupClick = onGroupClick,
                    onSongLongClick = onSongLongClick,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    uiState.editState?.let { editState ->
        TrackEditDialog(
            editState = editState,
            onTitleChange = onEditTitleChange,
            onArtistChange = onEditArtistChange,
            onSave = onSaveEdit,
            onDismiss = onDismissEdit,
        )
    }

    uiState.trackActionsState?.let { actionsState ->
        TrackActionsSheet(
            actionsState = actionsState,
            onRename = onRenameFromActions,
            onDelete = onDeleteFromActions,
            onAddToPlaylist = onAddToPlaylist,
            onToggleFavorite = { song ->
                onFavoriteClick(song)
                onDismissTrackActions()
            },
            onDismiss = onDismissTrackActions,
        )
    }

    uiState.deleteConfirmationSong?.let { song ->
        DeleteTrackDialog(
            song = song,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun LibraryViewTabs(
    selectedView: LibraryView,
    onViewSelected: (LibraryView) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = LibraryView.entries.indexOf(selectedView),
        modifier = modifier.fillMaxWidth(),
    ) {
        LibraryView.entries.forEach { view ->
            Tab(
                selected = view == selectedView,
                onClick = { onViewSelected(view) },
                text = { Text(text = view.label) },
            )
        }
    }
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        label = { Text(text = "Search library") },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
    )
}

@Composable
private fun LibraryFilters(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label) },
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    isImporting: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Songs",
            style = MaterialTheme.typography.titleLarge,
        )
        Button(
            onClick = onImportClick,
            enabled = !isImporting,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Import")
        }
    }
}

@Composable
private fun ImportError(
    message: String,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onClearError) {
            Text(text = "Dismiss")
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No songs yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Import local audio to build your library.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onSongClick: (Song, List<Song>) -> Unit,
    onGroupClick: (LibraryView, LibraryGroup) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        when (uiState.selectedView) {
            LibraryView.SONGS -> {
                items(
                    items = uiState.songs,
                    key = { song -> song.id },
                ) { song ->
                    SongRow(
                        song = song,
                        onClick = { onSongClick(song, uiState.songs) },
                        onLongClick = { onSongLongClick(song) },
                        onFavoriteClick = { onFavoriteClick(song) },
                    )
                    HorizontalDivider()
                }
            }
            LibraryView.ARTISTS -> {
                items(
                    items = uiState.artistGroups,
                    key = { group -> group.name.lowercase() },
                ) { group ->
                    LibraryGroupRow(
                        group = group,
                        onClick = { onGroupClick(LibraryView.ARTISTS, group) },
                    )
                    HorizontalDivider()
                }
            }
            LibraryView.ALBUMS -> {
                items(
                    items = uiState.albumGroups,
                    key = { group -> group.name.lowercase() },
                ) { group ->
                    LibraryGroupRow(
                        group = group,
                        onClick = { onGroupClick(LibraryView.ALBUMS, group) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LibraryGroupDetail(
    selectedGroup: SelectedLibraryGroup,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = selectedGroup.group
    val songs = group.songs

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to ${selectedGroup.view.label}",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${group.subtitle} - ${formatDuration(songs.sumOf { it.durationMs })}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(
                items = songs,
                key = { song -> song.id },
            ) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song, songs) },
                    onLongClick = { onSongLongClick(song) },
                    onFavoriteClick = { onFavoriteClick(song) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LibraryGroupRow(
    group: LibraryGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = group.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = group.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Text(
                text = formatDuration(group.songs.sumOf { it.durationMs }),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatDuration(song.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (song.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription = if (song.isFavorite) {
                            "Remove favorite"
                        } else {
                            "Add favorite"
                        },
                        tint = if (song.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        },
    )
}

private val LibraryUiState.isSelectedViewEmpty: Boolean
    get() = when (selectedView) {
        LibraryView.SONGS -> songs.isEmpty()
        LibraryView.ARTISTS -> artistGroups.isEmpty()
        LibraryView.ALBUMS -> albumGroups.isEmpty()
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrackActionsSheet(
    actionsState: TrackActionsState,
    onRename: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = actionsState.song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = actionsState.song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            ActionSheetRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                    )
                },
                label = "Rename",
                onClick = { onRename(actionsState.song) },
            )
            ActionSheetRow(
                icon = {
                    Icon(
                        imageVector = if (actionsState.song.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription = null,
                    )
                },
                label = if (actionsState.song.isFavorite) {
                    "Remove favorite"
                } else {
                    "Add favorite"
                },
                onClick = { onToggleFavorite(actionsState.song) },
            )
            ActionSheetRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                label = "Delete from library",
                onClick = { onDelete(actionsState.song) },
                textColor = MaterialTheme.colorScheme.error,
            )

            Text(
                text = "Add to playlist",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            if (actionsState.playlists.isEmpty()) {
                Text(
                    text = "No playlists yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                actionsState.playlists.forEach { playlist ->
                    ActionSheetRow(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                            )
                        },
                        label = playlist.name,
                        onClick = { onAddToPlaylist(playlist) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = icon,
        headlineContent = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor ?: MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

@Composable
private fun DeleteTrackDialog(
    song: Song,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete track?") },
        text = {
            Text(
                text = "Remove \"${song.title}\" from your library and playlists. Sona's stored audio copy will also be deleted.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun TrackEditDialog(
    editState: TrackEditState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit track") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editState.title,
                    onValueChange = onTitleChange,
                    label = { Text(text = "Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editState.artist,
                    onValueChange = onArtistChange,
                    label = { Text(text = "Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (editState.artistSuggestions.isNotEmpty()) {
                    Text(
                        text = "Existing artists",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        editState.artistSuggestions.forEach { artist ->
                            FilterChip(
                                selected = false,
                                onClick = { onArtistChange(artist) },
                                label = { Text(text = artist) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = editState.title.isNotBlank() && editState.artist.isNotBlank(),
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
