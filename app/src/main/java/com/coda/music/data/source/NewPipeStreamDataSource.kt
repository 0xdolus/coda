package com.coda.music.data.source

import com.coda.music.data.model.StreamQuality
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
 * Purpose: Resolves a YouTube Video ID to a [StreamResult] via NewPipeExtractor.
 * Respects [StreamQuality] to pick the appropriate bitrate tier.
 *
 * Quality selection (within one resolution call — not a retry):
 *   LOW    → lowest available bitrate
 *   NORMAL → closest to 128 kbps
 *   HIGH   → closest to 160 kbps
 *   BEST   → highest available bitrate (default)
 *
 * IOException and TimeoutCancellationException are re-thrown for
 * NewPipeRepository to map to UiError.
 *
 * Dependencies: NewPipeExtractor, Kotlin Coroutines
 * Public API: getStreamUrl(trackId, quality)
 * Future TODOs: brief URL caching (TTL ~5 min) to avoid redundant extraction.
 */
@Singleton
class NewPipeStreamDataSource @Inject constructor() : StreamProvider {

    // Called by PlayerController when quality setting changes mid-playback
    suspend fun getStreamUrl(
        trackId: String,
        quality: StreamQuality = StreamQuality.BEST
    ): StreamResult {
        return withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                try {
                    val url = "https://www.youtube.com/watch?v=$trackId"
                    val service = NewPipe.getServiceByUrl(url)
                    val linkHandler = service.streamLHFactory.fromUrl(url)
                    val extractor: StreamExtractor = service.getStreamExtractor(linkHandler)
                    extractor.fetchPage()

                    val streams = extractor.audioStreams
                        ?.filter { !it.content.isNullOrBlank() }
                        ?: emptyList()

                    if (streams.isEmpty()) return@withTimeout StreamResult.Unavailable

                    val picked = pickByQuality(streams, quality)
                        ?: return@withTimeout StreamResult.Unavailable

                    StreamResult.Ready(url = picked.content)
                } catch (e: TimeoutCancellationException) {
                    throw e
                } catch (e: IOException) {
                    throw e
                } catch (e: ExtractionException) {
                    val msg = e.message?.lowercase() ?: ""
                    if (msg.contains("age") || msg.contains("restricted") ||
                        msg.contains("region") || msg.contains("not available")
                    ) {
                        StreamResult.Restricted(reason = e.message ?: "Content restricted")
                    } else {
                        StreamResult.Unavailable
                    }
                }
            }
        }
    }

    // StreamProvider interface — defaults to BEST for callers that don't pass quality
    override suspend fun getStreamUrl(trackId: String): StreamResult =
        getStreamUrl(trackId, StreamQuality.BEST)

    private fun pickByQuality(streams: List<AudioStream>, quality: StreamQuality): AudioStream? {
        val withBitrate = streams.mapNotNull { s ->
            val br = s.averageBitrate.takeIf { it > 0 } ?: return@mapNotNull null
            Pair(br, s)
        }.sortedBy { it.first }

        if (withBitrate.isEmpty()) return streams.firstOrNull()

        return when (quality) {
            StreamQuality.LOW  -> withBitrate.first().second
            StreamQuality.BEST -> withBitrate.last().second
            StreamQuality.NORMAL -> withBitrate.minByOrNull { kotlin.math.abs(it.first - 128) }?.second
            StreamQuality.HIGH   -> withBitrate.minByOrNull { kotlin.math.abs(it.first - 160) }?.second
        }
    }
}
