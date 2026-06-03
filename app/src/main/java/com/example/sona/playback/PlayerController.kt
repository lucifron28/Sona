package com.example.sona.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.sona.domain.model.Song
import java.util.concurrent.ExecutionException
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
    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()
    private var mediaController: MediaController? = null
    private var queue: List<Song> = emptyList()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(errorMessage = error.message ?: "Playback failed.")
            }
        }
    }

    private val controllerFuture = MediaController.Builder(
        applicationContext,
        SessionToken(
            applicationContext,
            ComponentName(applicationContext, SonaPlaybackService::class.java),
        ),
    ).buildAsync()

    init {
        controllerFuture.addListener(
            {
                runCatchingControllerFuture()
                    ?.also { controller ->
                        mediaController = controller
                        controller.addListener(playerListener)
                        drainPendingActions(controller)
                        syncState()
                    }
            },
            ContextCompat.getMainExecutor(applicationContext),
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

        withController { controller ->
            controller.setMediaItems(
                playbackQueue.map { it.toMediaItem() },
                startIndex,
                C.TIME_UNSET,
            )
            controller.prepare()
            controller.play()
            syncState()
        }
    }

    fun playPause() {
        withController { controller ->
            if (controller.mediaItemCount == 0) return@withController

            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
            syncState()
        }
    }

    fun seekTo(positionMs: Long) {
        withController { controller ->
            if (controller.mediaItemCount == 0) return@withController

            controller.seekTo(positionMs.coerceAtLeast(0L))
            syncState()
        }
    }

    fun skipNext() {
        withController { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                controller.play()
                syncState()
            }
        }
    }

    fun skipPrevious() {
        withController { controller ->
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
                controller.play()
            } else {
                controller.seekTo(0L)
            }
            syncState()
        }
    }

    fun stop() {
        withController { controller ->
            controller.stop()
            syncState()
        }
    }

    fun release() {
        scope.cancel()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        pendingActions.clear()
    }

    private fun withController(action: (MediaController) -> Unit) {
        val controller = mediaController
        if (controller != null) {
            action(controller)
        } else {
            pendingActions.addLast(action)
        }
    }

    private fun drainPendingActions(controller: MediaController) {
        while (pendingActions.isNotEmpty()) {
            pendingActions.removeFirst()(controller)
        }
    }

    private fun runCatchingControllerFuture(): MediaController? = try {
        controllerFuture.get()
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        recordControllerConnectionError(error)
        null
    } catch (error: ExecutionException) {
        recordControllerConnectionError(error)
        null
    }

    private fun recordControllerConnectionError(error: Throwable) {
        _playbackState.update {
            it.copy(errorMessage = error.cause?.message ?: error.message ?: "Playback service unavailable.")
        }
    }

    private fun syncState() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex.takeIf {
            controller.mediaItemCount > 0 && it >= 0
        } ?: -1
        val currentSong = queue.getOrNull(currentIndex)
        val playerDuration = controller.duration
        val durationMs = when {
            playerDuration != C.TIME_UNSET && playerDuration >= 0L -> playerDuration
            currentSong != null -> currentSong.durationMs
            else -> 0L
        }

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
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
