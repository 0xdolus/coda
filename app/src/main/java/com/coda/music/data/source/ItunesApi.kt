package com.coda.music.data.source

import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {

    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 1
    ): ItunesSearchResponse
}

data class ItunesSearchResponse(val results: List<ItunesResult>)
data class ItunesResult(val artworkUrl100: String?)
