package com.coda.music.ui.state

/**
 * Purpose: UI events for HomeScreen.
 *
 * OnTrendingClick(videoId) routes to the same "player/{trackId}" destination
 * as OnTrackClick — TrendingVideo.id and Track.id share the same YouTube
 * Video ID space (see "ID Source of Truth"). No separate trending route exists.
 *
 * Dependencies: none
 * Public API: sealed interface
 * Future TODOs: none
 */
sealed interface HomeEvent {
    data class OnArtistClick(val artistId: String) : HomeEvent
    data class OnTrackClick(val trackId: String) : HomeEvent
    data class OnTrendingClick(val videoId: String) : HomeEvent
}
