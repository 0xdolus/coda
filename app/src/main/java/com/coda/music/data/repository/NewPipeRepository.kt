package com.coda.music.data.repository

import com.coda.music.data.model.Artist
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.model.UiError
import com.coda.music.data.source.NewPipeSearchDataSource
import com.coda.music.data.source.NewPipeStreamDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purpose: Implements [MusicRepository] by orchestrating
 * [NewPipeSearchDataSource] and [NewPipeStreamDataSource].
 *
 * In-memory cache: artists, topSongs, and search results are cached for
 * the lifetime of the app process (@Singleton scope). This eliminates
 * redundant NewPipe calls on tab switches and back navigation, cutting
 * the perceived latency from ~5s cold to near-instant on repeat visits.
 * Cache is intentionally not persisted to disk — Phase 2 can add Room/
 * DataStore if offline-first behaviour is needed.
 *
 * Top songs query: uses "top music songs" + music genre filter to avoid
 * the YouTube trending feed returning gaming/vlog content.
 *
 * Dependencies: NewPipeSearchDataSource, NewPipeStreamDataSource
 * Future TODOs:
 *   - toggleLike(trackId): Phase 2 — DataStore-backed liked track IDs.
 *   - Disk cache / TTL-based invalidation for offline-first.
 */
@Singleton
class NewPipeRepository @Inject constructor(
    private val searchDataSource: NewPipeSearchDataSource,
    private val streamDataSource: NewPipeStreamDataSource
) : MusicRepository {

    // In-memory cache — valid for the lifetime of the process
    private var cachedArtists: List<Artist>? = null
    private var cachedTopSongs: List<Track>? = null
    private val cachedSearchResults = mutableMapOf<String, List<Track>>()

    override suspend fun getArtists(): List<Artist> {
        cachedArtists?.let { return it }
        val tracks = searchDataSource.searchTracks("popular music artists official")
        val result = tracks
            .distinctBy { it.artistName }
            .take(20)
            .map { track ->
                Artist(
                    id               = track.id,
                    name             = track.artistName,
                    imageUrl         = track.imageUrl,
                    monthlyListeners = "Unknown listeners"
                )
            }
        cachedArtists = result
        return result
    }

    override suspend fun getTopSongs(): List<Track> {
        cachedTopSongs?.let { return it }
        val result = searchDataSource.getTopSongs()
        cachedTopSongs = result
        return result
    }

    override suspend fun getArtistInfo(artistId: String): Artist {
        return searchDataSource.getArtistInfo(artistId)
    }

    override suspend fun getArtistSongs(artistId: String): List<Track> {
        return searchDataSource.getArtistSongs(artistId)
    }

    override suspend fun search(query: String): List<Track> {
        cachedSearchResults[query]?.let { return it }
        val result = searchDataSource.searchTracks(query)
        if (result.isNotEmpty()) cachedSearchResults[query] = result
        return result
    }

    override suspend fun getLikedSongs(): List<Track> {
        // Phase 2: DataStore-backed liked track IDs — deferred.
        return emptyList()
    }

    override suspend fun getTrack(trackId: String): Track {
        return searchDataSource.getTrackInfo(trackId)
    }

    override suspend fun getTrackStreamUrl(trackId: String): StreamResult {
        return try {
            streamDataSource.getStreamUrl(trackId)
        } catch (e: ExtractionException) {
            StreamResult.Unavailable
        }
    }

    fun mapToUiError(e: Exception): UiError {
        if (e is CancellationException && e !is TimeoutCancellationException) throw e
        val message = when (e) {
            is IOException -> "Check your connection"
            is TimeoutCancellationException -> "Request timed out"
            is ExtractionException -> "Unable to load content"
            else -> "Something went wrong"
        }
        return UiError(message)
    }
}
