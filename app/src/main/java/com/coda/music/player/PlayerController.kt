package com.coda.music.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.coda.music.data.model.StreamQuality
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.source.NewPipeStreamDataSource
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purpose: Singleton owner of PlaybackState. Connects to MediaPlaybackService
 *          via Media3 MediaController so all playback commands are routed through
 *          the foreground service — keeping audio alive in the background.
 *          Translates Player.Listener callbacks into PlaybackState updates.
 *
 * Dependencies: MediaPlaybackService, MediaController (media3-session),
 *               NewPipeStreamDataSource, Hilt
 *
 * Public API:
 *   playbackState: StateFlow<PlaybackState>
 *   play(track, queue, index, quality)
 *   reloadWithQuality(quality)
 *   togglePlayPause()
 *   seekTo(positionSeconds)
 *   skipToNext() / skipToPrevious()
 *   toggleShuffle() / toggleRepeat()
 *   release()
 *
 * Future TODOs:
 *   - Audio focus edge-case handling beyond ExoPlayer built-in
 *   - Queue sync: push queue to MediaSession so lock-screen controls show next track
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamDataSource: NewPipeStreamDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var progressJob: Job? = null
    private var currentQuality: StreamQuality = StreamQuality.BEST

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            if (isPlaying) startProgressTracking() else stopProgressTracking()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) handleTrackEnded()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = controller ?: return
            _playbackState.value = _playbackState.value.copy(
                durationSeconds = (player.duration / 1000).toInt().coerceAtLeast(0)
            )
        }
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    fun play(
        track: Track,
        queue: List<Track> = listOf(track),
        queueIndex: Int = 0,
        quality: StreamQuality = currentQuality
    ) {
        currentQuality = quality
        scope.launch {
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = queueIndex,
                isPlaying = false,
                progressSeconds = 0,
                durationSeconds = 0
            )
            resolveAndPlay(track, quality)
        }
    }

    /** Re-resolves the currently playing track at a new quality setting. */
    fun reloadWithQuality(quality: StreamQuality) {
        currentQuality = quality
        val track = _playbackState.value.currentTrack ?: return
        val positionMs = controller?.currentPosition ?: 0L
        scope.launch {
            resolveAndPlay(track, quality, seekToMs = positionMs)
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionSeconds: Int) {
        controller?.seekTo(positionSeconds * 1000L)
        _playbackState.value = _playbackState.value.copy(progressSeconds = positionSeconds)
    }

    fun skipToNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val nextIndex = if (state.repeatEnabled) {
            (state.currentIndex + 1) % queue.size
        } else {
            if (state.currentIndex + 1 < queue.size) state.currentIndex + 1 else return
        }
        play(queue[nextIndex], queue, nextIndex)
    }

    fun skipToPrevious() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val prevIndex = (state.currentIndex - 1).coerceAtLeast(0)
        play(queue[prevIndex], queue, prevIndex)
    }

    fun toggleShuffle() {
        val state = _playbackState.value
        val nowEnabled = !state.shuffleEnabled
        if (nowEnabled && state.queue.size > 1) {
            val current = state.queue[state.currentIndex]
            val rest = state.queue.toMutableList().also { it.removeAt(state.currentIndex) }
            rest.shuffle()
            val newQueue = mutableListOf(current) + rest
            _playbackState.value = state.copy(shuffleEnabled = true, queue = newQueue, currentIndex = 0)
        } else {
            _playbackState.value = state.copy(shuffleEnabled = nowEnabled)
        }
    }

    fun toggleRepeat() {
        _playbackState.value = _playbackState.value.copy(
            repeatEnabled = !_playbackState.value.repeatEnabled
        )
    }

    fun release() {
        stopProgressTracking()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    private suspend fun resolveAndPlay(track: Track, quality: StreamQuality, seekToMs: Long = 0L) {
        val player = controller ?: return
        when (val result = streamDataSource.getStreamUrl(track.id, quality)) {
            is StreamResult.Ready -> {
                val mediaItem = MediaItem.fromUri(result.url)
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(mediaItem)
                player.prepare()
                if (seekToMs > 0) player.seekTo(seekToMs)
                player.play()
            }
            is StreamResult.Unavailable -> {
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            }
            is StreamResult.Restricted -> {
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            }
        }
    }

    private fun handleTrackEnded() {
        if (_playbackState.value.repeatEnabled) {
            controller?.seekTo(0)
            controller?.play()
        } else {
            skipToNext()
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val player = controller
                if (player != null) {
                    val progress = (player.currentPosition / 1000).toInt()
                    val duration = (player.duration / 1000).toInt().coerceAtLeast(0)
                    _playbackState.value = _playbackState.value.copy(
                        progressSeconds = progress,
                        durationSeconds = duration
                    )
                }
                delay(1_000)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
