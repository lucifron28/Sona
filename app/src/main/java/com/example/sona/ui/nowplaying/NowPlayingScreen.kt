package com.example.sona.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sona.core.utils.formatDuration
import com.example.sona.domain.model.Song
import com.example.sona.playback.PlaybackRepeatMode
import com.example.sona.playback.PlaybackState
import com.example.sona.ui.components.SonaDefaultAlbumArt

@Composable
fun NowPlayingScreen(
    contentPadding: PaddingValues,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onQueueSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playbackState.currentSong == null) {
        EmptyNowPlaying(
            contentPadding = contentPadding,
            modifier = modifier,
        )
        return
    }

    val currentSong = playbackState.currentSong
    val durationMs = playbackState.durationMs.coerceAtLeast(0L)
    val positionMs = playbackState.positionMs
        .coerceAtLeast(0L)
        .coerceAtMost(durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SonaDefaultAlbumArt(
            isPlaying = playbackState.isPlaying,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .padding(top = 8.dp),
            spinLogoWithVinyl = true
        )
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = currentSong.artist,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        QueueLabel(
            queueIndex = playbackState.queueIndex,
            queueSize = playbackState.queueSize,
            modifier = Modifier.padding(top = 8.dp),
        )

        PlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek,
        )

        PlaybackModeControls(
            isShuffleEnabled = playbackState.isShuffleEnabled,
            repeatMode = playbackState.repeatMode,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
        )

        PlaybackControls(
            isPlaying = playbackState.isPlaying,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
        )

        QueueList(
            queue = playbackState.queue,
            currentSongId = currentSong.id,
            onQueueSongClick = onQueueSongClick,
            modifier = Modifier.fillMaxWidth(),
        )

        playbackState.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyNowPlaying(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Choose a song from Library.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QueueLabel(
    queueIndex: Int,
    queueSize: Int,
    modifier: Modifier = Modifier,
) {
    if (queueIndex < 0 || queueSize <= 0) return

    Text(
        text = "${queueIndex + 1} of $queueSize",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderMax = durationMs.takeIf { it > 0L } ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = positionMs.toFloat().coerceIn(0f, sliderMax.toFloat()),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..sliderMax.toFloat(),
            enabled = durationMs > 0L,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaybackModeControls(
    isShuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = if (isShuffleEnabled) {
                    "Disable shuffle"
                } else {
                    "Enable shuffle"
                },
                tint = if (isShuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onCycleRepeatMode) {
            Icon(
                imageVector = when (repeatMode) {
                    PlaybackRepeatMode.ONE -> Icons.Filled.RepeatOne
                    PlaybackRepeatMode.OFF,
                    PlaybackRepeatMode.ALL,
                    -> Icons.Filled.Repeat
                },
                contentDescription = when (repeatMode) {
                    PlaybackRepeatMode.OFF -> "Repeat off"
                    PlaybackRepeatMode.ALL -> "Repeat all"
                    PlaybackRepeatMode.ONE -> "Repeat one"
                },
                tint = if (repeatMode == PlaybackRepeatMode.OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipPrevious) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
            )
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onSkipNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
            )
        }
    }
}

@Composable
private fun QueueList(
    queue: List<Song>,
    currentSongId: Long,
    onQueueSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) return

    Column(modifier = modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        queue.forEachIndexed { index, song ->
            val isCurrent = song.id == currentSongId
            ListItem(
                modifier = Modifier.clickable { onQueueSongClick(song) },
                leadingContent = {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                headlineContent = {
                    Text(
                        text = song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                supportingContent = {
                    Text(
                        text = if (isCurrent) {
                            "Now playing - ${song.artist}"
                        } else {
                            song.artist
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Text(
                        text = formatDuration(song.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            if (index < queue.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
