package com.example.sona.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.sona.data.database.SonaDatabase
import com.example.sona.data.repository.SongRepository
import com.example.sona.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SonaPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }
    private val songRepository by lazy {
        SongRepository(SonaDatabase.create(applicationContext).songDao())
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var restorePending = true
    private var restoreTimelineChangePending = false
    private var positionPersistenceJob: Job? = null
    private var restoringMediaId: String? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId
            if (mediaId != null && mediaId == restoringMediaId) {
                restoringMediaId = null
                return
            }

            if (mediaId != restoringMediaId) restoringMediaId = null
            mediaId?.toLongOrNull()?.let { songId ->
                serviceScope.launch(Dispatchers.IO) {
                    songRepository.recordPlayed(songId, System.currentTimeMillis())
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            persistPlaybackState()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (restoreTimelineChangePending) {
                restoreTimelineChangePending = false
            } else if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                restorePending = false
                startPositionPersistence()
            }
            persistPlaybackState()
        }

    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val createdPlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
        player = createdPlayer
        mediaSession = MediaSession.Builder(this, createdPlayer).build()
        restorePlaybackState(createdPlayer)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        if ((player?.mediaItemCount ?: 0) > 0) restorePending = false
        persistPlaybackState()
        positionPersistenceJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        player = null
        mediaSession = null
        super.onDestroy()
    }

    private fun restorePlaybackState(player: ExoPlayer) {
        serviceScope.launch {
            val savedIds = preferences.getString(KEY_QUEUE_IDS, null)
                .orEmpty()
                .split(',')
                .mapNotNull { it.toLongOrNull() }
            val savedIndex = preferences.getInt(KEY_CURRENT_INDEX, -1)
            val savedPosition = preferences.getLong(KEY_POSITION_MS, 0L).coerceAtLeast(0L)
            val savedShuffle = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)
            val savedRepeatMode = preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)
                .takeIf {
                    it == Player.REPEAT_MODE_ONE ||
                        it == Player.REPEAT_MODE_ALL ||
                        it == Player.REPEAT_MODE_OFF
                } ?: Player.REPEAT_MODE_OFF
            val shouldResume = preferences.getBoolean(KEY_PLAY_WHEN_READY, false)

            val songs = try {
                withContext(Dispatchers.IO) { songRepository.getSongs(savedIds) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                return@launch
            }

            if (restorePending && player.mediaItemCount == 0 && songs.isNotEmpty()) {
                val songsById = songs.associateBy(Song::id)
                val restoredIds = savedIds.filter(songsById::containsKey)
                val targetOriginalIndex = when {
                    savedIndex in savedIds.indices &&
                        savedIds[savedIndex] in songsById -> savedIndex
                    else -> savedIds.indices.firstOrNull {
                        it >= savedIndex.coerceAtLeast(0) && savedIds[it] in songsById
                    } ?: savedIds.indices.lastOrNull {
                        it < savedIndex && savedIds[it] in songsById
                    } ?: 0
                }
                val restoredIndex = savedIds.take(targetOriginalIndex)
                    .count(songsById::containsKey)
                val restoredQueue = restoredIds.mapNotNull(songsById::get)
                val restoredPosition = if (
                    savedIndex in savedIds.indices &&
                    savedIds[savedIndex] in songsById
                ) {
                    savedPosition
                } else {
                    0L
                }

                player.shuffleModeEnabled = savedShuffle
                player.repeatMode = savedRepeatMode
                restoringMediaId = restoredQueue[restoredIndex].id.toString()
                restoreTimelineChangePending = true
                player.setMediaItems(
                    restoredQueue.map { it.toMediaItem() },
                    restoredIndex,
                    restoredPosition,
                )
                player.prepare()
                if (shouldResume) player.play()
            }

            restorePending = false
            persistPlaybackState()
            startPositionPersistence()
        }
    }

    private fun startPositionPersistence() {
        if (positionPersistenceJob?.isActive == true) return
        positionPersistenceJob = serviceScope.launch {
            while (isActive) {
                persistPlaybackState()
                delay(POSITION_PERSISTENCE_INTERVAL_MS)
            }
        }
    }

    private fun persistPlaybackState() {
        if (restorePending) return
        val player = player ?: return
        val queueIds = (0 until player.mediaItemCount).joinToString(",") {
            player.getMediaItemAt(it).mediaId
        }
        val currentIndex = player.currentMediaItemIndex
            .takeIf { it in 0 until player.mediaItemCount } ?: -1
        val position = if (currentIndex >= 0) player.currentPosition.coerceAtLeast(0L) else 0L

        preferences.edit()
            .putString(KEY_QUEUE_IDS, queueIds)
            .putInt(KEY_CURRENT_INDEX, currentIndex)
            .putLong(KEY_POSITION_MS, position)
            .putBoolean(KEY_SHUFFLE_ENABLED, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT_MODE, player.repeatMode)
            .putBoolean(KEY_PLAY_WHEN_READY, player.playWhenReady)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "sona_playback_state"
        const val KEY_QUEUE_IDS = "queue_ids"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_PLAY_WHEN_READY = "play_when_ready"
        const val POSITION_PERSISTENCE_INTERVAL_MS = 1_000L
    }
}
