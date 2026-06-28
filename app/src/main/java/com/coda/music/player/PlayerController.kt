package com.coda.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.coda.music.data.model.StreamQuality
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.source.NewPipeStreamDataSource
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
 * Purpose: Singleton owner of PlaybackState and the ExoPlayer instance.
 * Translates ExoPlayer listener callbacks into PlaybackState updates.
 *
 * Now accepts StreamQuality directly so the Settings screen can trigger
 * re-resolution of the current track at a different bitrate immediately.
 *
 * Dependencies: ExoPlayer, MediaSession, NewPipeStreamDataSource, Hilt
 * Public API:
 *   playbackState: StateFlow<PlaybackState>
 *   play(track, queue, index, quality)
 *   reloadWithQuality(quality)   ← re-resolves current track at new quality
 *   togglePlayPause()
 *   seekTo(positionSeconds)
 *   skipToNext() / skipToPrevious()
 *   toggleShuffle() / toggleRepeat()
 *   release()
 * Future TODOs:
 *   - MediaPlaybackService + foreground notification
 *   - Audio focus edge case handling beyond ExoPlayer built-in
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamDataSource: NewPipeStreamDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    val mediaSession: MediaSession = MediaSession.Builder(context, exoPlayer).build()

    private var progressJob: Job? = null
    private var currentQuality: StreamQuality = StreamQuality.BEST

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) handleTrackEnded()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playbackState.value = _playbackState.value.copy(
                    durationSeconds = (exoPlayer.duration / 1000).toInt().coerceAtLeast(0)
                )
            }
        })
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
        val positionMs = exoPlayer.currentPosition
        scope.launch {
            resolveAndPlay(track, quality, seekToMs = positionMs)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekTo(positionSeconds: Int) {
        exoPlayer.seekTo(positionSeconds * 1000L)
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
        _playbackState.value = _playbackState.value.copy(repeatEnabled = !_playbackState.value.repeatEnabled)
    }

    fun release() {
        stopProgressTracking()
        mediaSession.release()
        exoPlayer.release()
    }

    private suspend fun resolveAndPlay(track: Track, quality: StreamQuality, seekToMs: Long = 0L) {
        when (val result = streamDataSource.getStreamUrl(track.id, quality)) {
            is StreamResult.Ready -> {
                val factory = if (result.headers.isNotEmpty()) {
                    DefaultHttpDataSource.Factory().setDefaultRequestProperties(result.headers)
                } else {
                    DefaultHttpDataSource.Factory()
                }
                val mediaItem = MediaItem.fromUri(result.url)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                if (seekToMs > 0) exoPlayer.seekTo(seekToMs)
                exoPlayer.play()
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
            exoPlayer.seekTo(0)
            exoPlayer.play()
        } else {
            skipToNext()
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val progress = (exoPlayer.currentPosition / 1000).toInt()
                val duration = (exoPlayer.duration / 1000).toInt().coerceAtLeast(0)
                _playbackState.value = _playbackState.value.copy(
                    progressSeconds = progress,
                    durationSeconds = duration
                )
                delay(1_000)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
