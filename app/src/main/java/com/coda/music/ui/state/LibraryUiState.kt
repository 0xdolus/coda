package com.coda.music.ui.state

import com.coda.music.data.model.Track

/**
 * Purpose: UI state for LibraryScreen.
 * getLikedSongs() returns empty list until toggleLike is implemented (Phase 2).
 *
 * Dependencies: Track
 * Public API: sealed interface with Loading, Success, Error
 * Future TODOs: populate once DataStore-backed toggleLike is added in Phase 2
 */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val likedSongs: List<Track>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
