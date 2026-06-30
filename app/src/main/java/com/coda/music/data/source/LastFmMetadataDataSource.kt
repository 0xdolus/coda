package com.coda.music.data.source

import com.coda.music.data.model.Artist
import com.coda.music.data.model.LastFmSearchTrack
import com.coda.music.data.model.LastFmTrack
import com.coda.music.data.model.Track
import com.coda.music.util.toCleanTrackTitle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmMetadataDataSource @Inject constructor(
    private val api: LastFmApi,
    private val artworkDataSource: ArtworkDataSource
) {
    suspend fun getTopTracks(): List<Track> = coroutineScope {
        api.getTopTracks().tracks.track
            .map { async { it.toTrack() } }
            .awaitAll()
    }

    suspend fun search(query: String): List<Track> = coroutineScope {
        api.searchTracks(query).results.trackMatches.track
            .map { async { it.toTrack() } }
            .awaitAll()
    }

    suspend fun getArtistInfo(name: String): Artist {
        val info = api.getArtistInfo(name).artist
        val lastFmImage = info.image?.lastOrNull()?.url?.takeIf { it.isNotBlank() }
        val imageUrl = lastFmImage ?: artworkDataSource.resolveArtistArtwork(info.name) ?: ""
        return Artist(
            id               = info.mbid?.takeIf { it.isNotBlank() } ?: info.name.lowercase(),
            name             = info.name,
            imageUrl         = imageUrl,
            monthlyListeners = info.stats?.listeners
                ?.toLongOrNull()
                ?.let { formatListenerCount(it) }
                ?: "Unknown listeners"
        )
    }

    private suspend fun LastFmTrack.toTrack(): Track {
        val cleanTitle = name.toCleanTrackTitle()
        val id = mbid?.takeIf { it.isNotBlank() }
            ?: "${artist.name}::$cleanTitle".lowercase()
        val lastFmImage = image?.lastOrNull()?.url?.takeIf { it.isNotBlank() }
        val imageUrl = lastFmImage
            ?: artworkDataSource.resolveTrackArtwork(artist.name, cleanTitle)
            ?: ""
        return Track(
            id              = id,
            title           = cleanTitle,
            artistName      = artist.name,
            imageUrl        = imageUrl,
            durationSeconds = duration?.toIntOrNull() ?: 0
        )
    }

    // Search results have artist as a plain string, not an object
    private suspend fun LastFmSearchTrack.toTrack(): Track {
        val cleanTitle = name.toCleanTrackTitle()
        val id = mbid?.takeIf { it.isNotBlank() }
            ?: "${artist}::$cleanTitle".lowercase()
        val lastFmImage = image?.lastOrNull()?.url?.takeIf { it.isNotBlank() }
        val imageUrl = lastFmImage
            ?: artworkDataSource.resolveTrackArtwork(artist, cleanTitle)
            ?: ""
        return Track(
            id              = id,
            title           = cleanTitle,
            artistName      = artist,
            imageUrl        = imageUrl,
            durationSeconds = duration?.toIntOrNull() ?: 0
        )
    }

    private fun formatListenerCount(count: Long): String = when {
        count >= 1_000_000 -> "%.1fM listeners".format(count / 1_000_000.0)
        count >= 1_000     -> "%.1fK listeners".format(count / 1_000.0)
        else               -> "$count listeners"
    }
}
