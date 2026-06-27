package com.coda.music.navigation

/**
 * Purpose: Defines all navigation route strings for AppNavGraph.
 *
 * Root routes (no args): home, search, library, more.
 * Parameterised routes: artist/{artistId}, player/{trackId}.
 *
 * There is NO separate trending-video route — TrendingVideo.id and Track.id
 * share the same YouTube Video ID space. HomeEvent.OnTrendingClick(videoId)
 * navigates to player/{videoId} exactly like OnTrackClick.
 *
 * AppNavGraph.kt is NOT generated in this pass. It references screen
 * composables that do not exist until PASS 8, and is generated in PASS 9.
 *
 * Dependencies: none
 * Public API: object with const String route values
 * Future TODOs: none
 */
object Routes {
    const val HOME    = "home"
    const val SEARCH  = "search"
    const val LIBRARY = "library"
    const val MORE    = "more"

    const val ARTIST  = "artist/{${NavArguments.ARTIST_ID}}"
    const val PLAYER  = "player/{${NavArguments.TRACK_ID}}"

    fun artist(artistId: String) = "artist/$artistId"
    fun player(trackId: String)  = "player/$trackId"
}
