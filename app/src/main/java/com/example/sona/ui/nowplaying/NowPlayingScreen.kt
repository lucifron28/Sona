package com.example.sona.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.sona.core.utils.formatDuration
import com.example.sona.domain.model.Song
import com.example.sona.playback.PlaybackRepeatMode
import com.example.sona.playback.PlaybackState
import com.example.sona.ui.components.SonaDefaultAlbumArt
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueSong: (Song) -> Unit,
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

        ExpandableQueuePanel(
            queue = playbackState.queue,
            currentSongId = currentSong.id,
            queueIndex = playbackState.queueIndex,
            onQueueSongClick = onQueueSongClick,
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueSong = onRemoveQueueSong,
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
private fun ExpandableQueuePanel(
    queue: List<Song>,
    currentSongId: Long,
    queueIndex: Int,
    onQueueSongClick: (Song) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueSong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(queue.size) {
        if (queue.isEmpty()) expanded = false
    }

    Column(modifier = modifier.padding(top = 8.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
            tonalElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = queueSummary(queueIndex = queueIndex, queueSize = queue.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse queue" else "Expand queue",
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
                    .clip(MaterialTheme.shapes.small),
            ) {
                queue.forEachIndexed { index, song ->
                    DraggableQueueRow(
                        index = index,
                        song = song,
                        isCurrent = song.id == currentSongId,
                        queueSize = queue.size,
                        onClick = { onQueueSongClick(song) },
                        onMove = onMoveQueueItem,
                        onRemove = { onRemoveQueueSong(song) },
                    )
                    if (index < queue.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableQueueRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    queueSize: Int,
    onClick: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { QueueRowHeight.toPx() }
    var dragOffsetX by remember(song.id) { mutableStateOf(0f) }
    var dragOffsetY by remember(song.id) { mutableStateOf(0f) }
    val revealTrash = abs(dragOffsetX) > TrashRevealDistancePx

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val removeThresholdPx = with(density) { maxWidth.toPx() * 0.34f }
        val trashAlignment = if (dragOffsetX >= 0f) Alignment.CenterStart else Alignment.CenterEnd

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = if (revealTrash) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        Color.Transparent
                    },
                ),
        ) {
            if (revealTrash) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier
                        .align(trashAlignment)
                        .padding(horizontal = 24.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        ListItem(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = dragOffsetX.roundToInt(),
                        y = dragOffsetY.roundToInt(),
                    )
                }
                .pointerInput(index, queueSize, song.id) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            dragOffsetY += dragAmount.y
                        },
                        onDragEnd = {
                            val shouldRemove = abs(dragOffsetX) >= removeThresholdPx
                            val targetIndex = (index + (dragOffsetY / rowHeightPx).roundToInt())
                                .coerceIn(0, queueSize - 1)

                            dragOffsetX = 0f
                            dragOffsetY = 0f

                            if (shouldRemove) {
                                onRemove()
                            } else if (targetIndex != index) {
                                onMove(index, targetIndex)
                            }
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                    )
                }
                .clickable(onClick = onClick),
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
    }
}

private fun queueSummary(
    queueIndex: Int,
    queueSize: Int,
): String = if (queueIndex >= 0) {
    "${queueIndex + 1} of $queueSize"
} else {
    "$queueSize tracks"
}

private val QueueRowHeight = 72.dp
private const val TrashRevealDistancePx = 12f
