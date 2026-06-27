package com.coda.music.player

import com.coda.music.data.model.Track

/**
 * Purpose: Single source of truth for all playback state exposed to the UI.
 * PlayerController is the sole owner of this data class and exposes it via
 * StateFlow<PlaybackState>. No screen or ViewModel may hold its own copy of
 * currentTrack or isPlaying — they must observe PlayerController exclusively.
 *
 * Dependencies: Track (data model)
 *
 * Public API: data class — all fields read-only, updated only by PlayerController.
 *
 * Queue Ownership:
 *   - queue + currentIndex are the single source of truth for what is playing
 *     and what comes next.
 *   - OnNext     → currentIndex + 1 (wraps to 0 if repeatEnabled, else stops)
 *   - OnPrevious → currentIndex - 1 (clamped at 0)
 *   - OnShuffle  → shuffles queue in place, excluding current position
 *   - A queue is populated whenever playback starts from any list context;
 *     the full visible list becomes the queue, currentIndex = tapped position.
 *
 * Default value: PlaybackState() — no track loaded, empty queue, index = -1.
 *
 * Future TODOs:
 *   - Add playbackSpeed: Float once variable-speed playback is in scope.
 *   - Add sleepTimerEndsAt: Long? when sleep timer feature is added.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progressSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1
)
