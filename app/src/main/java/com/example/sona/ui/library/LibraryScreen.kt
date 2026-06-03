package com.example.sona.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sona.core.utils.formatDuration
import com.example.sona.domain.model.Song

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    uiState: LibraryUiState,
    onImportAudio: (android.net.Uri) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onViewSelected: (LibraryView) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onEditClick: (Song) -> Unit,
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

        if (uiState.isSelectedViewEmpty) {
            EmptyLibrary(modifier = Modifier.weight(1f))
        } else {
            LibraryContent(
                uiState = uiState,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick,
                onEditClick = onEditClick,
                modifier = Modifier.weight(1f),
            )
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
    onFavoriteClick: (Song) -> Unit,
    onEditClick: (Song) -> Unit,
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
                        onFavoriteClick = { onFavoriteClick(song) },
                        onEditClick = { onEditClick(song) },
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
                        onClick = {
                            group.songs.firstOrNull()?.let { firstSong ->
                                onSongClick(firstSong, group.songs)
                            }
                        },
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
                        onClick = {
                            group.songs.firstOrNull()?.let { firstSong ->
                                onSongClick(firstSong, group.songs)
                            }
                        },
                    )
                    HorizontalDivider()
                }
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
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
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
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit track details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
