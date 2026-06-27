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
 * [NewPipeSearchDataSource] (metadata, search, trending) and
 * [NewPipeStreamDataSource] (stream resolution).
 *
 * This class NEVER calls NewPipe APIs directly. All extraction is
 * delegated to the two data sources. NewPipeRepository is the seam between
 * the MusicRepository interface and the NewPipe-specific data sources.
 *
 * The canonical exception → UiError message mapping lives here in
 * [mapToUiError] — not duplicated per catch block, not spread across
 * callers. No catch block in this class may use a bare e.message string
 * for a known exception type.
 *
 * Exception → UiError mapping:
 *   IOException                  → "Check your connection"
 *   TimeoutCancellationException → "Request timed out"
 *   ExtractionException          → "Unable to load content"
 *   Any other Exception          → "Something went wrong"
 *
 * Dependencies: NewPipeSearchDataSource, NewPipeStreamDataSource,
 *               MusicRepository (implements), Kotlin Coroutines
 *
 * Public API: implements MusicRepository; exposes mapToUiError() for
 *             ViewModels to convert thrown exceptions into UiError values.
 *
 * Future TODOs:
 *   - toggleLike(trackId): Phase 2 — DataStore-backed (datastore-preferences),
 *     storing a Set<String> of liked track IDs. getLikedSongs() will resolve
 *     those IDs back to Track via getTrackInfo()/NewPipe.
 *   - getArtists(): currently a broad search fallback; a channel-browse
 *     approach may improve relevance once the NewPipe layer is proven.
 */
@Singleton
class NewPipeRepository @Inject constructor(
    private val searchDataSource: NewPipeSearchDataSource,
    private val streamDataSource: NewPipeStreamDataSource
) : MusicRepository {

    override suspend fun getArtists(): List<Artist> {
        // NewPipe has no "list all artists" endpoint. Delegate to a broad
        // top-music search and extract unique uploaders as a best-effort
        // artist list until a better source is available.
        val tracks = searchDataSource.searchTracks("top music artists")
        return tracks
            .distinctBy { it.artistName }
            .map { track ->
                Artist(
                    id               = track.id, // best-effort; real channel ID needs getArtistInfo
                    name             = track.artistName,
                    imageUrl         = track.imageUrl,
                    monthlyListeners = "Unknown listeners"
                )
            }
    }

    override suspend fun getTopSongs(): List<Track> {
        return searchDataSource.getTopSongs()
    }

    override suspend fun getArtistSongs(artistId: String): List<Track> {
        return searchDataSource.getArtistSongs(artistId)
    }

    override suspend fun search(query: String): List<Track> {
        return searchDataSource.searchTracks(query)
    }

    override suspend fun getLikedSongs(): List<Track> {
        // Phase 2: DataStore-backed (datastore-preferences) — deferred.
        // Stores a Set<String> of liked track IDs; resolves back to Track
        // via getTrack(). Not Room — overkill for a single set of IDs.
        return emptyList()
    }

    override suspend fun getTrack(trackId: String): Track {
        return searchDataSource.getTrackInfo(trackId)
    }

    /**
     * Resolves a track's stream URL. Returns [StreamResult] — never a raw String.
     * Stream failures are isolated here; they never affect metadata queries.
     *
     * [ExtractionException] from the stream layer is converted to
     * [StreamResult.Unavailable] rather than propagated — the stream
     * layer's ExtractionException is distinct from a metadata failure
     * and should not surface to the UI as a critical error.
     *
     * [IOException] and [TimeoutCancellationException] propagate to the
     * calling ViewModel, which maps them via [mapToUiError].
     */
    override suspend fun getTrackStreamUrl(trackId: String): StreamResult {
        return try {
            streamDataSource.getStreamUrl(trackId)
        } catch (e: ExtractionException) {
            StreamResult.Unavailable
        }
    }

    /**
     * Canonical exception → [UiError] mapping.
     *
     * Single source of truth for user-facing error messages. ViewModels
     * catch exceptions thrown from repository calls and pass them here to
     * get the correct UiError to emit on their error SharedFlow.
     *
     * CancellationException (non-timeout) is re-thrown untouched to
     * preserve coroutine cooperative cancellation.
     */
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
