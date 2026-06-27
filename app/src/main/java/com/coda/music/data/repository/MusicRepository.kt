package com.coda.music.data.repository

import com.coda.music.data.model.Artist
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track

interface MusicRepository {
    suspend fun getArtists(): List<Artist>
    suspend fun getTopSongs(): List<Track>
    suspend fun getArtistInfo(artistId: String): Artist
    suspend fun getArtistSongs(artistId: String): List<Track>
    suspend fun search(query: String): List<Track>
    suspend fun getLikedSongs(): List<Track>
    suspend fun getTrack(trackId: String): Track
    suspend fun getTrackStreamUrl(trackId: String): StreamResult
}
