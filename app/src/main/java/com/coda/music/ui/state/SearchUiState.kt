package com.coda.music.ui.state

import com.coda.music.data.model.Track

/**
 * Purpose: UI state for SearchScreen.
 *
 * Idle is shown when query is empty (0 characters). browseCategories belong
 * here — they are the empty-query UI, not a search result.
 *
 * Empty is its own state for zero-result queries — never represented as
 * Success(query, emptyList()). "Nothing found" is a different UI than
 * "results loaded."
 *
 * Dependencies: Track
 * Public API: sealed interface with Idle, Loading, Success, Empty, Error
 * Future TODOs: none
 */
sealed interface SearchUiState {
    data class Idle(
        val browseCategories: List<String> = listOf(
            "Soul", "Hip-Hop", "R&B", "Indie", "Pop", "Alternative"
        )
    ) : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val query: String,
        val results: List<Track> // always non-empty — see Empty
    ) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
