package com.coda.music.data.provider

import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.Track

/**
 * Contract for resolving a Track to a playable stream URL.
 * NewPipe implements this by searching YouTube with "{artist} - {title}".
 * Track.id is a Last.fm identifier as of Phase 1 — never a YouTube video ID.
 */
interface StreamProvider {
    suspend fun getStreamUrl(track: Track): StreamResult
}
