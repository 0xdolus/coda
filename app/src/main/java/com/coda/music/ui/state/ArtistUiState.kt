package com.coda.music.ui.state

import com.coda.music.data.model.Artist
import com.coda.music.data.model.Playlist
import com.coda.music.data.model.Track

/**
 * Purpose: UI state for ArtistScreen.
 * monthlyListeners lives on Artist directly — not a separate field here.
 *
 * Dependencies: Artist, Track, Playlist
 * Public API: sealed interface with Loading, Success, Error
 * Future TODOs: none
 */
sealed interface ArtistUiState {
    data object Loading : ArtistUiState
    data class Success(
        val artist: Artist,
        val songs: List<Track>,
        val playlists: List<Playlist>
    ) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}
