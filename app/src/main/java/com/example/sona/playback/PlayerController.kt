package com.example.sona.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.sona.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var queue: List<Song> = emptyList()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    syncState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackState.update {
                        it.copy(errorMessage = error.message ?: "Playback failed.")
                    }
                }
            },
        )

        scope.launch {
            while (isActive) {
                syncState()
                delay(500L)
            }
        }
    }

    fun play(song: Song, queue: List<Song>) {
        val playbackQueue = queue.ifEmpty { listOf(song) }
        val startIndex = playbackQueue.indexOfFirst { it.id == song.id }
            .takeIf { it >= 0 }
            ?: 0

        this.queue = playbackQueue
        _playbackState.update { it.copy(errorMessage = null) }
        player.setMediaItems(
            playbackQueue.map { it.toMediaItem() },
            startIndex,
            C.TIME_UNSET,
        )
        player.prepare()
        player.play()
        syncState()
    }

    fun playPause() {
        if (player.mediaItemCount == 0) return

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        syncState()
    }

    fun seekTo(positionMs: Long) {
        if (player.mediaItemCount == 0) return

        player.seekTo(positionMs.coerceAtLeast(0L))
        syncState()
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
            syncState()
        }
    }

    fun skipPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0L)
        }
        syncState()
    }

    fun stop() {
        player.stop()
        syncState()
    }

    fun release() {
        scope.cancel()
        player.release()
    }

    private fun syncState() {
        val currentIndex = player.currentMediaItemIndex.takeIf { it >= 0 } ?: -1
        val currentSong = queue.getOrNull(currentIndex)
        val playerDuration = player.duration
        val durationMs = when {
            playerDuration != C.TIME_UNSET && playerDuration >= 0L -> playerDuration
            currentSong != null -> currentSong.durationMs
            else -> 0L
        }

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = durationMs,
                queueIndex = currentIndex,
                queueSize = queue.size,
            )
        }
    }
}

private fun Song.toMediaItem(): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)

    artworkUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(uri))
        .setMediaMetadata(metadataBuilder.build())
        .build()
}
