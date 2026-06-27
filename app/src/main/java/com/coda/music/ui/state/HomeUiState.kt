package com.coda.music.ui.state

import com.coda.music.data.model.Artist
import com.coda.music.data.model.Track
import com.coda.music.data.model.TrendingVideo

/**
 * Purpose: UI state for HomeScreen.
 *
 * currentTrack and isPlaying are NOT here. HomeScreen reads playback state
 * from LocalPlayerController directly via MiniPlayer. No ViewModel owns a
 * copy of playback state — that rule is absolute.
 *
 * Dependencies: Artist, Track, TrendingVideo
 * Public API: sealed interface with Loading, Success, Error
 * Future TODOs: none
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val artists: List<Artist>,
        val trendingVideos: List<TrendingVideo>,
        val topSongs: List<Track>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
