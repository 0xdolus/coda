package com.coda.music.ui.state

/**
 * Purpose: UI events for LibraryScreen.
 * Dependencies: none
 * Public API: sealed interface
 * Future TODOs: add OnLikeToggle once Phase 2 toggleLike is implemented
 */
sealed interface LibraryEvent {
    data class OnTrackClick(val trackId: String) : LibraryEvent
}
