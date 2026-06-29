package com.coda.music.data.source

import com.coda.music.BuildConfig
import com.coda.music.data.model.LastFmArtistInfoResponse
import com.coda.music.data.model.LastFmSearchResponse
import com.coda.music.data.model.LastFmTopTracksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApi {

    @GET("?method=chart.gettoptracks&format=json")
    suspend fun getTopTracks(
        @Query("api_key") apiKey: String = BuildConfig.LASTFM_API_KEY,
        @Query("limit") limit: Int = 30
    ): LastFmTopTracksResponse

    @GET("?method=track.search&format=json")
    suspend fun searchTracks(
        @Query("track") query: String,
        @Query("api_key") apiKey: String = BuildConfig.LASTFM_API_KEY,
        @Query("limit") limit: Int = 30
    ): LastFmSearchResponse

    @GET("?method=artist.getinfo&format=json")
    suspend fun getArtistInfo(
        @Query("artist") name: String,
        @Query("api_key") apiKey: String = BuildConfig.LASTFM_API_KEY
    ): LastFmArtistInfoResponse
}
