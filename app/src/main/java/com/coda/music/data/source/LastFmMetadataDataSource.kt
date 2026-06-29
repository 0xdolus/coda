package com.coda.music.data.source

import com.coda.music.data.model.Artist
import com.coda.music.data.model.LastFmTrack
import com.coda.music.data.model.Track
import com.coda.music.util.toCleanTrackTitle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmMetadataDataSource @Inject constructor(
    private val api: LastFmApi
) {
    suspend fun getTopTracks(): List<Track> =
        api.getTopTracks().tracks.track.map { it.toTrack() }

    suspend fun search(query: String): List<Track> =
        api.searchTracks(query).results.trackMatches.track.map { it.toTrack() }

    suspend fun getArtistInfo(name: String): Artist {
        val info = api.getArtistInfo(name).artist
        return Artist(
            id               = info.mbid?.takeIf { it.isNotBlank() } ?: info.name.lowercase(),
            name             = info.name,
            imageUrl         = info.image?.lastOrNull()?.url ?: "",
            monthlyListeners = info.stats?.listeners
                ?.toLongOrNull()
                ?.let { formatListenerCount(it) }
                ?: "Unknown listeners"
        )
    }

    private fun LastFmTrack.toTrack(): Track {
        val cleanTitle = name.toCleanTrackTitle()
        val id = mbid?.takeIf { it.isNotBlank() }
            ?: "${artist.name}::$cleanTitle".lowercase()
        return Track(
            id              = id,
            title           = cleanTitle,
            artistName      = artist.name,
            imageUrl        = image?.lastOrNull()?.url ?: "",
            durationSeconds = duration?.toIntOrNull() ?: 0
        )
    }

    private fun formatListenerCount(count: Long): String = when {
        count >= 1_000_000 -> "%.1fM listeners".format(count / 1_000_000.0)
        count >= 1_000     -> "%.1fK listeners".format(count / 1_000.0)
        else               -> "$count listeners"
    }
}
