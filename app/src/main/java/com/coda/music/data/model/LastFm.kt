package com.coda.music.data.model

import com.squareup.moshi.Json

// Used by chart.gettoptracks — artist is an object
data class LastFmTrack(
    val name: String,
    val mbid: String?,
    val artist: LastFmArtistRef,
    val duration: String?,
    val image: List<LastFmImage>?
)

// Used by track.search — artist is a plain string
data class LastFmSearchTrack(
    val name: String,
    val mbid: String?,
    val artist: String,
    val duration: String?,
    val image: List<LastFmImage>?
)

data class LastFmArtistRef(
    val name: String,
    val mbid: String?
)

data class LastFmImage(
    @Json(name = "#text") val url: String,
    val size: String
)

data class LastFmArtist(
    val name: String,
    val mbid: String?,
    val image: List<LastFmImage>?,
    val stats: LastFmArtistStats?
)

data class LastFmArtistStats(
    val listeners: String?
)

data class LastFmTopTracksResponse(val tracks: LastFmTracksWrapper)
data class LastFmTracksWrapper(val track: List<LastFmTrack>)

data class LastFmSearchResponse(val results: LastFmSearchResultsWrapper)
data class LastFmSearchResultsWrapper(
    @Json(name = "trackmatches") val trackMatches: LastFmTrackMatches
)
data class LastFmTrackMatches(val track: List<LastFmSearchTrack>)

data class LastFmArtistInfoResponse(val artist: LastFmArtist)
