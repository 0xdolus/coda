package com.coda.music.data.source

import com.coda.music.data.model.StreamQuality
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track
import com.coda.music.data.provider.StreamProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stream-only as of Phase 1. Searches YouTube by "{artist} - {title}" and
 * resolves audio from the best match. Never supplies metadata, home feed,
 * artist info, or search results — that is Last.fm's job now.
 */
@Singleton
class NewPipeStreamDataSource @Inject constructor() : StreamProvider {

    suspend fun getStreamUrl(
        track: Track,
        quality: StreamQuality = StreamQuality.BEST
    ): StreamResult {
        return withContext(Dispatchers.IO) {
            withTimeout(10_000L) {
                try {
                    val service = NewPipe.getService(ServiceList.YouTube.serviceId)
                    val searchString = "${track.artistName} - ${track.title}"
                    val queryHandler = service.searchQHFactory
                        .fromQuery(searchString, listOf("videos"), "")
                    val searchExtractor = service.getSearchExtractor(queryHandler)
                    searchExtractor.fetchPage()
                    val firstResult = searchExtractor.initialPage.items
                        .filterIsInstance<StreamInfoItem>()
                        .firstOrNull()
                        ?: return@withTimeout StreamResult.Unavailable

                    val linkHandler = service.streamLHFactory.fromUrl(firstResult.url)
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

    override suspend fun getStreamUrl(track: Track): StreamResult =
        getStreamUrl(track, StreamQuality.BEST)

    private fun pickByQuality(streams: List<AudioStream>, quality: StreamQuality): AudioStream? {
        val withBitrate = streams.mapNotNull { s ->
            val br = s.averageBitrate.takeIf { it > 0 } ?: return@mapNotNull null
            Pair(br, s)
        }.sortedBy { it.first }

        if (withBitrate.isEmpty()) return streams.firstOrNull()

        return when (quality) {
            StreamQuality.LOW    -> withBitrate.first().second
            StreamQuality.BEST   -> withBitrate.last().second
            StreamQuality.NORMAL -> withBitrate.minByOrNull { kotlin.math.abs(it.first - 128) }?.second
            StreamQuality.HIGH   -> withBitrate.minByOrNull { kotlin.math.abs(it.first - 160) }?.second
        }
    }
}
