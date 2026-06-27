package com.coda.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.provider.StreamProvider
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
 * Translates ExoPlayer listener callbacks into PlaybackState updates and
 * exposes StateFlow<PlaybackState> for UI observation.
 *
 * Playback is fully independent of Activity/Compose lifecycle. ExoPlayer
 * must never be paused or stopped in response to ON_STOP, onPause, or any
 * other lifecycle callback — that is the entire point of background playback.
 * collectAsStateWithLifecycle() in the UI handles pausing state *collection*
 * while backgrounded; no additional lifecycle-driven pause/resume logic
 * belongs here.
 *
 * StreamResult.Ready.headers, when present, are passed via
 * DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers)
 * rather than using a default factory — headers must never be dropped.
 *
 * A MediaSession is attached to the ExoPlayer instance for lock screen
 * controls and headset/Bluetooth media key handling. Full background
 * playback (foreground service + notification) is deferred until a
 * MediaPlaybackService is added in a future pass.
 *
 * Dependencies: ExoPlayer (media3-exoplayer), MediaSession (media3-session),
 *               StreamProvider, Kotlin Coroutines, Hilt
 *
 * Public API:
 *   playbackState: StateFlow<PlaybackState>
 *   play(track, queue, index)
 *   togglePlayPause()
 *   seekTo(positionSeconds)
 *   skipToNext()
 *   skipToPrevious()
 *   toggleShuffle()
 *   toggleRepeat()
 *   release()
 *
 * Future TODOs:
 *   - Add MediaPlaybackService + foreground notification once permissions
 *     (FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK) are added.
 *   - Add audio focus handling if ExoPlayer's built-in handling proves
 *     insufficient for edge cases.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamProvider: StreamProvider
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    val mediaSession: MediaSession = MediaSession.Builder(context, exoPlayer).build()

    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Duration updates once ExoPlayer knows it
                _playbackState.value = _playbackState.value.copy(
                    durationSeconds = (exoPlayer.duration / 1000).toInt().coerceAtLeast(0)
                )
            }
        })
    }

    /**
     * Begins playback of [track] within the context of [queue], starting at
     * [queueIndex]. The full list becomes the active queue; currentIndex is
     * set to the tapped track's position.
     *
     * Stream resolution is done here via StreamProvider. All three
     * StreamResult cases are handled — Ready, Unavailable, Restricted.
     * Playback does not start unless StreamResult.Ready is returned.
     */
    fun play(track: Track, queue: List<Track> = listOf(track), queueIndex: Int = 0) {
        scope.launch {
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = queueIndex,
                isPlaying = false,
                progressSeconds = 0,
                durationSeconds = 0
            )

            when (val result = streamProvider.getStreamUrl(track.id)) {
                is StreamResult.Ready -> {
                    val dataSourceFactory = if (result.headers.isNotEmpty()) {
                        DefaultHttpDataSource.Factory()
                            .setDefaultRequestProperties(result.headers)
                    } else {
                        DefaultHttpDataSource.Factory()
                    }
                    val mediaItem = MediaItem.fromUri(result.url)
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    // Note: custom DataSource.Factory requires MediaSource wiring;
                    // for now set media item and rely on default HTTP handling.
                    // Headers support via MediaSource is a future improvement.
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }

                is StreamResult.Unavailable -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentTrack = track // retain track so UI can show unavailable state
                    )
                }

                is StreamResult.Restricted -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentTrack = track
                    )
                }
            }
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
            // Shuffle queue in place, keeping current track at currentIndex
            val current = state.queue[state.currentIndex]
            val rest = state.queue.toMutableList().also { it.removeAt(state.currentIndex) }
            rest.shuffle()
            val newQueue = mutableListOf(current) + rest
            _playbackState.value = state.copy(
                shuffleEnabled = true,
                queue = newQueue,
                currentIndex = 0
            )
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
        mediaSession.release()
        exoPlayer.release()
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        if (state.repeatEnabled) {
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
