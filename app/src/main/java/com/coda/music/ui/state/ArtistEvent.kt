package com.coda.music.ui.state

/**
 * Purpose: UI events for ArtistScreen.
 * Dependencies: none
 * Public API: sealed interface
 * Future TODOs: none
 */
sealed interface ArtistEvent {
    data class OnTrackClick(val trackId: String) : ArtistEvent
    data object OnBackClick : ArtistEvent
}
