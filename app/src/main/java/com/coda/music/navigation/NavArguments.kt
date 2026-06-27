package com.coda.music.navigation

/**
 * Purpose: Defines NavBackStackEntry argument key constants used in route
 * templates and SavedStateHandle lookups.
 *
 * ARTIST_ID — YouTube Channel ID (see "ID Source of Truth" in coda_sot.md)
 * TRACK_ID  — YouTube Video ID
 *
 * These constants are the single source of truth for argument key strings.
 * Every SavedStateHandle lookup in ArtistViewModel and PlayerViewModel must
 * reference these constants — never inline string literals.
 *
 * Dependencies: none
 * Public API: object with const String key values
 * Future TODOs: none
 */
object NavArguments {
    const val ARTIST_ID = "artistId"
    const val TRACK_ID  = "trackId"
}
