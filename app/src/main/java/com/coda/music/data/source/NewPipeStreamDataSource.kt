package com.coda.music.data.source

import com.coda.music.data.model.StreamResult
import com.coda.music.data.provider.StreamProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purpose: Resolves a YouTube Video ID (Track.id) to a playable [StreamResult]
 * via NewPipeExtractor. This is the sole class responsible for stream URL
 * resolution. It never touches search or metadata — that is
 * NewPipeSearchDataSource's domain. Isolation prevents stream failures from
 * poisoning metadata queries and makes this layer independently testable.
 *
 * Stream resolution is the most failure-prone layer in this app. Within a
 * single resolution call, alternate audio formats/qualities are tried before
 * giving up. This is expected defensive coding — NOT a retry (retrying a
 * failed call is forbidden per Error Handling Strategy). ExtractionException
 * is caught broadly; NewPipeExtractor throws many specific subtypes
 * (age-restriction, content removed, regional blocks, etc.) and catching the
 * broad type is intentional.
 *
 * Every extraction call is wrapped in withContext(Dispatchers.IO) +
 * withTimeout(10_000). NewPipe can hang indefinitely — both are mandatory.
 *
 * Dependencies: NewPipeExtractor (via NewPipe.init in CodaApplication),
 *               Kotlin Coroutines
 *
 * Public API: getStreamUrl(trackId: String): StreamResult
 *
 * Future TODOs:
 *   - If NewPipe ever exposes a more reliable stream resolution path, adopt it here.
 *   - Consider caching resolved URLs briefly (TTL ~5 min) to avoid redundant
 *     extraction on repeated playback of the same track.
 */
@Singleton
class NewPipeStreamDataSource @Inject constructor() : StreamProvider {

    /**
     * Resolves a YouTube Video ID to a [StreamResult].
     *
     * Returns:
     *   [StreamResult.Ready]       — a valid audio stream URL ready for ExoPlayer.
     *   [StreamResult.Unavailable] — no usable stream found (removed, no audio, etc).
     *   [StreamResult.Restricted]  — age-gated or region-blocked content.
     *
     * IOException and TimeoutCancellationException are re-thrown so
     * NewPipeRepository can apply the canonical exception → UiError mapping.
     */
    override suspend fun getStreamUrl(trackId: String): StreamResult {
        return withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                try {
                    val url = "https://www.youtube.com/watch?v=$trackId"
                    val service = NewPipe.getServiceByUrl(url)
                    val linkHandler = service.streamLHFactory.fromUrl(url)
                    val extractor: StreamExtractor = service.getStreamExtractor(linkHandler)
                    extractor.fetchPage()

                    val audioStream: AudioStream? = pickBestAudioStream(extractor)

                    if (audioStream != null && !audioStream.content.isNullOrBlank()) {
                        StreamResult.Ready(url = audioStream.content)
                    } else {
                        StreamResult.Unavailable
                    }
                } catch (e: TimeoutCancellationException) {
                    throw e // re-throw — mapped in NewPipeRepository
                } catch (e: IOException) {
                    throw e // re-throw — mapped in NewPipeRepository
                } catch (e: ExtractionException) {
                    // Broadly catch all NewPipe subtypes intentionally.
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("age") || msg.contains("restricted") ||
                        msg.contains("region") || msg.contains("not available in your country")
                    ) {
                        StreamResult.Restricted(reason = e.message ?: "Content restricted")
                    } else {
                        StreamResult.Unavailable
                    }
                }
            }
        }
    }

    /**
     * Picks the best available audio-only stream.
     * Priority order within a single resolution call (not a retry):
     *   1. Opus/WebM (best quality per bit)
     *   2. M4A
     *   3. Any other audio-only stream with a non-blank content URL
     *
     * Returns null only if no audio stream at all is available.
     */
    private fun pickBestAudioStream(extractor: StreamExtractor): AudioStream? {
        return try {
            val streams = extractor.audioStreams
            if (streams.isNullOrEmpty()) return null

            streams.firstOrNull {
                it.format?.name?.contains("OPUS", ignoreCase = true) == true &&
                    !it.content.isNullOrBlank()
            } ?: streams.firstOrNull {
                it.format?.name?.contains("M4A", ignoreCase = true) == true &&
                    !it.content.isNullOrBlank()
            } ?: streams.firstOrNull { !it.content.isNullOrBlank() }
        } catch (e: ExtractionException) {
            null
        }
    }
}
