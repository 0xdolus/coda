package com.coda.music.ui.state

/**
 * Purpose: UI events for SearchScreen.
 * Dependencies: none
 * Public API: sealed interface
 * Future TODOs: none
 */
sealed interface SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent
    data class OnTrackClick(val trackId: String) : SearchEvent
    data class OnCategoryClick(val category: String) : SearchEvent
}
