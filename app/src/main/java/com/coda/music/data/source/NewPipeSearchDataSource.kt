package com.coda.music.data.source

import com.coda.music.data.model.Artist
import com.coda.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

private const val TOP_SONGS_COUNT = 30
private const val TIMEOUT_MS      = 10_000L

@Singleton
class NewPipeSearchDataSource @Inject constructor() {

    private val youTubeService get() = NewPipe.getService(ServiceList.YouTube.serviceId)

    suspend fun searchTracks(query: String): List<Track> =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                val linkHandler = youTubeService
                    .searchQHFactory
                    .fromQuery(query, listOf("videos"), "")
                val info = SearchInfo.getInfo(youTubeService, linkHandler)
                info.getRelatedItems()
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toTrack() }
            }
        }

    suspend fun getArtistInfo(channelId: String): Artist =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                val url  = "https://www.youtube.com/channel/$channelId"
                val info = ChannelInfo.getInfo(youTubeService, url)
                Artist(
                    id               = channelId,
                    name             = info.name,
                    imageUrl         = info.avatars.firstOrNull()?.url ?: "",
                    monthlyListeners = info.subscriberCount
                        .takeIf { it >= 0 }
                        ?.let { formatSubscriberCount(it) }
                        ?: "Unknown listeners"
                )
            }
        }

    suspend fun getArtistSongs(channelId: String): List<Track> =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                val url  = "https://www.youtube.com/channel/$channelId/videos"
                val info = ChannelInfo.getInfo(youTubeService, url)
                info.getRelatedItems()
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toTrack() }
            }
        }

    suspend fun getTopSongs(): List<Track> =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                try {
                    val kioskList = youTubeService.kioskList
                    val kioskId   = kioskList.availableKiosks.firstOrNull()
                    if (kioskId != null) {
                        val linkHandler = kioskList
                            .getListLinkHandlerFactoryByType(kioskId)
                            .fromId(kioskId)
                        val info = KioskInfo.getInfo(youTubeService, linkHandler.url)
                        val tracks = info.getRelatedItems()
                            .filterIsInstance<StreamInfoItem>()
                            .take(TOP_SONGS_COUNT)
                            .map { it.toTrack() }
                        if (tracks.isNotEmpty()) return@withTimeout tracks
                    }
                } catch (_: Exception) {}

                // Fallback: search("top music")
                val linkHandler = youTubeService
                    .searchQHFactory
                    .fromQuery("top music", listOf("videos"), "")
                SearchInfo.getInfo(youTubeService, linkHandler)
                    .getRelatedItems()
                    .filterIsInstance<StreamInfoItem>()
                    .take(TOP_SONGS_COUNT)
                    .map { it.toTrack() }
            }
        }

    suspend fun getTrackInfo(videoId: String): Track =
        withContext(Dispatchers.IO) {
            withTimeout(TIMEOUT_MS) {
                val url  = "https://www.youtube.com/watch?v=$videoId"
                val info = StreamInfo.getInfo(youTubeService, url)
                Track(
                    id              = videoId,
                    title           = info.name,
                    artistName      = info.uploaderName,
                    imageUrl        = info.thumbnails.firstOrNull()?.url ?: "",
                    durationSeconds = info.duration.toInt()
                )
            }
        }

    private fun StreamInfoItem.toTrack(): Track {
        val videoId = url
            .substringAfter("watch?v=")
            .substringBefore("&")
            .ifBlank { url }
        return Track(
            id              = videoId,
            title           = name,
            artistName      = uploaderName ?: "",
            imageUrl        = thumbnails.firstOrNull()?.url ?: "",
            durationSeconds = duration.toInt()
        )
    }

    private fun formatSubscriberCount(count: Long): String = when {
        count >= 1_000_000 -> "%.1fM monthly listeners".format(count / 1_000_000.0)
        count >= 1_000     -> "%.1fK monthly listeners".format(count / 1_000.0)
        else               -> "$count monthly listeners"
    }
}
