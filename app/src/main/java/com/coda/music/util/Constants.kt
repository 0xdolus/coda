package com.coda.music.util

object CodaConstants {
    const val DEEP_LINK_SCHEME   = "coda"
    const val NEWPIPE_TIMEOUT_MS = 10_000L

    // Last.fm — metadata only, never used for stream resolution.
    // Key is injected at build time via BuildConfig.LASTFM_API_KEY,
    // sourced from local.properties (never committed to VCS).
    const val LASTFM_BASE_URL  = "https://ws.audioscrobbler.com/2.0/"
    const val LASTFM_TIMEOUT_MS = 10_000L
}
