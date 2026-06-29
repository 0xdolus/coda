package com.coda.music.data.repository

import com.coda.music.data.model.Artist
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.model.UiError
import com.coda.music.data.source.LastFmMetadataDataSource
import com.coda.music.data.source.NewPipeStreamDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata (artists, top songs, search, artist info) → LastFmMetadataDataSource.
 * Stream resolution → NewPipeStreamDataSource (searches YouTube by artist+title).
 *
 * getArtists() fans out artist.getinfo calls with async/awaitAll rather than
 * serial map — Last.fm free tier allows 5 req/sec so parallel is safe and
 * cuts the cold load from ~4s to ~1s.
 *
 * getArtistInfo(artistId): artistId is either an mbid or a lowercase name slug
 * (see LastFmMetadataDataSource). Phase 1 requires a clean launch — any
 * pre-Phase-1 YouTube channel IDs in the nav backstack will not resolve via
 * Last.fm. Since IDs are not persisted to disk this is safe in practice.
 *
 * getTrack(trackId) falls back to cache only — a deep-link to a track with
 * no prior cache entry will throw NoSuchElementException. Phase 1.1 should
 * add a Last.fm track.getInfo?mbid= lookup for the miss path.
 *
 * getArtistSongs uses artist name search as a stand-in for
 * artist.getTopTracks — swap in once LastFmApi gains that endpoint.
 *
 * Future TODOs:
 *   - toggleLike(trackId): Phase 2 — DataStore-backed liked track IDs.
 *   - Disk cache / TTL-based invalidation for offline-first.
 */
@Singleton
class NewPipeRepository @Inject constructor(
    private val metadataDataSource: LastFmMetadataDataSource,
    private val streamDataSource: NewPipeStreamDataSource
) : MusicRepository {

    private var cachedArtists: List<Artist>? = null
    private var cachedTopSongs: List<Track>? = null
    private val cachedSearchResults = mutableMapOf<String, List<Track>>()

    override suspend fun getArtists(): List<Artist> {
        cachedArtists?.let { return it }
        val topTracks = metadataDataSource.getTopTracks()
        val distinctArtistNames = topTracks
            .distinctBy { it.artistName }
            .take(20)
            .map { it.artistName }

        // Fan out in parallel — Last.fm free tier is 5 req/sec, 20 parallel
        // calls is fine and cuts cold load from ~4s serial to ~1s.
        val result = coroutineScope {
            distinctArtistNames.map { name ->
                async {
                    runCatching { metadataDataSource.getArtistInfo(name) }
                        .getOrElse {
                            val matchingTrack = topTracks.first { it.artistName == name }
                            Artist(
                                id               = name.lowercase(),
                                name             = name,
                                imageUrl         = matchingTrack.imageUrl,
                                monthlyListeners = "Unknown listeners"
                            )
                        }
                }
            }.awaitAll()
        }
        cachedArtists = result
        return result
    }

    override suspend fun getTopSongs(): List<Track> {
        cachedTopSongs?.let { return it }
        val result = metadataDataSource.getTopTracks()
        cachedTopSongs = result
        return result
    }

    override suspend fun getArtistInfo(artistId: String): Artist =
        metadataDataSource.getArtistInfo(artistId)

    override suspend fun getArtistSongs(artistId: String): List<Track> =
        metadataDataSource.search(artistId)

    override suspend fun search(query: String): List<Track> {
        cachedSearchResults[query]?.let { return it }
        val result = metadataDataSource.search(query)
        if (result.isNotEmpty()) cachedSearchResults[query] = result
        return result
    }

    override suspend fun getLikedSongs(): List<Track> =
        emptyList() // Phase 2: DataStore-backed liked track IDs

    override suspend fun getTrack(trackId: String): Track {
        return cachedTopSongs?.find { it.id == trackId }
            ?: cachedSearchResults.values.flatten().find { it.id == trackId }
            ?: throw NoSuchElementException("Track not found in cache: $trackId")
    }

    override suspend fun getTrackStreamUrl(track: Track): StreamResult {
        return try {
            streamDataSource.getStreamUrl(track)
        } catch (e: ExtractionException) {
            StreamResult.Unavailable
        }
    }

    fun mapToUiError(e: Exception): UiError {
        if (e is CancellationException && e !is TimeoutCancellationException) throw e
        val message = when (e) {
            is IOException               -> "Check your connection"
            is TimeoutCancellationException -> "Request timed out"
            is ExtractionException       -> "Unable to load content"
            is HttpException             -> "Unable to load content"
            is NoSuchElementException    -> "Track not found"
            else                         -> "Something went wrong"
        }
        return UiError(message)
    }
}
