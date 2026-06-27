package com.coda.music.ui.state

import com.coda.music.data.model.Track

/**
 * Purpose: UI state for PlayerScreen.
 *
 * shuffleEnabled and repeatEnabled are sourced from PlaybackState via
 * PlayerController — PlayerViewModel observes PlayerController and maps
 * those fields here. They must never be duplicated or owned independently.
 *
 * TrackUnavailable is the preferred state over null/exception propagation
 * when StreamResult.Unavailable or StreamResult.Restricted is returned.
 *
 * Dependencies: Track
 * Public API: sealed interface with Loading, Success, TrackUnavailable, Error
 * Future TODOs: none
 */
sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(
        val currentTrack: Track,
        val isPlaying: Boolean,
        val isLiked: Boolean,
        val progressSeconds: Int,
        val durationSeconds: Int,
        val shuffleEnabled: Boolean,
        val repeatEnabled: Boolean
    ) : PlayerUiState
    data class TrackUnavailable(val reason: String) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
