package com.coda.music.data.source

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkDataSource @Inject constructor(
    private val api: ItunesApi
) {
    // Cache by lookup key so repeat tracks/artists never re-hit iTunes.
    // Null is cached too, so a confirmed miss doesn't get retried every scroll.
    private val cache = mutableMapOf<String, String?>()
    private val mutex = Mutex()

    suspend fun resolveTrackArtwork(artistName: String, trackTitle: String): String? =
        resolve(cacheKey = "track:$artistName:$trackTitle", term = "$artistName $trackTitle")

    suspend fun resolveArtistArtwork(artistName: String): String? =
        resolve(cacheKey = "artist:$artistName", term = artistName)

    private suspend fun resolve(cacheKey: String, term: String): String? {
        mutex.withLock {
            if (cache.containsKey(cacheKey)) return cache[cacheKey]
        }

        val resolved = runCatching {
            api.search(term = term, limit = 1)
                .results
                .firstOrNull()
                ?.artworkUrl100
                ?.upsize()
        }.getOrNull()

        mutex.withLock { cache[cacheKey] = resolved }
        return resolved
    }

    // iTunes bakes resolution into the URL path itself, e.g. .../100x100bb.jpg
    private fun String.upsize(targetPx: Int = 600): String =
        replace(Regex("""\d+x\d+bb"""), "${targetPx}x${targetPx}bb")
}
