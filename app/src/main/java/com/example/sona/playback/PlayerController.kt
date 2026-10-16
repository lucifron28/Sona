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
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.SongRepository
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
import kotlinx.coroutines.withContext

class PlayerController(context: Context) {
    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()
    private var mediaController: MediaController? = null
    private val songRepository by lazy {
        SongRepository(SonaDatabase.create(applicationContext).songDao())
    }
    private var queueLoadRequest = 0
    private var queue: List<Song> = emptyList()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                reconcileQueue(player)
            }
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
                        reconcileQueue(controller)
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

    fun toggleShuffle() {
        withController { controller ->
            controller.shuffleModeEnabled = controller.shuffleModeEnabled.not()
            syncState()
        }
    }

    fun cycleRepeatMode() {
        withController { controller ->
            controller.repeatMode = controller.repeatMode
                .toPlaybackRepeatMode()
                .next()
                .toPlayerRepeatMode()
            syncState()
        }
    }

    fun playQueueSong(song: Song) {
        val index = queue.indexOfFirst { it.id == song.id }
        if (index < 0) return

        withController { controller ->
            if (index >= controller.mediaItemCount) return@withController

            controller.seekToDefaultPosition(index)
            controller.play()
            syncState()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val nextQueue = queue.moveItem(fromIndex, toIndex)
        if (nextQueue === queue) return

        queue = nextQueue
        withController { controller ->
            if (fromIndex >= controller.mediaItemCount || toIndex >= controller.mediaItemCount) {
                syncState()
                return@withController
            }

            controller.moveMediaItem(fromIndex, toIndex)
            syncState()
        }
    }

    fun removeQueueItem(song: Song) {
        val index = queue.indexOfFirst { it.id == song.id }
        if (index < 0) return

        queue = queue.removeIndex(index)
        withController { controller ->
            if (index < controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
            if (queue.isEmpty()) {
                controller.clearMediaItems()
            } else if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            syncState()
        }
    }

    fun restoreQueueItem(song: Song, index: Int) {
        val insertionIndex = index.coerceIn(0, queue.size)
        queue = queue.insertItem(insertionIndex, song)
        withController { controller ->
            controller.addMediaItem(insertionIndex, song.toMediaItem())
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
                controller.play()
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

    private fun reconcileQueue(controller: Player) {
        val mediaItemCount = controller.mediaItemCount
        val mediaIds = ArrayList<Long>(mediaItemCount)
        for (index in 0 until mediaItemCount) {
            mediaIds += controller.getMediaItemAt(index).mediaId.toLongOrNull() ?: return
        }
        var queueMatches = mediaIds.size == queue.size
        if (queueMatches) {
            for (index in mediaIds.indices) {
                if (mediaIds[index] != queue[index].id) {
                    queueMatches = false
                    break
                }
            }
        }
        if (queueMatches) return

        val request = ++queueLoadRequest
        if (mediaIds.isEmpty()) {
            queue = emptyList()
            syncState()
            return
        }

        scope.launch {
            val restoredQueue = withContext(Dispatchers.IO) {
                songRepository.getSongs(mediaIds)
            }
            if (request != queueLoadRequest || mediaController !== controller) return@launch

            val currentMediaIds = ArrayList<Long>(controller.mediaItemCount)
            for (index in 0 until controller.mediaItemCount) {
                val id = controller.getMediaItemAt(index).mediaId.toLongOrNull() ?: return@launch
                currentMediaIds += id
            }
            if (currentMediaIds != mediaIds) return@launch

            queue = restoredQueue
            syncState()
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
                queue = queue,
                isPlaying = controller.isPlaying,
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode.toPlaybackRepeatMode(),
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = durationMs,
                queueIndex = currentIndex,
                queueSize = queue.size,
            )
        }
    }
}

internal fun Song.toMediaItem(): MediaItem {
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

private fun Int.toPlaybackRepeatMode(): PlaybackRepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
    Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
    else -> PlaybackRepeatMode.OFF
}

private fun PlaybackRepeatMode.toPlayerRepeatMode(): Int = when (this) {
    PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
    PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
    PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
}
