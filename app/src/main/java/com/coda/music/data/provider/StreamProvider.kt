package com.coda.music.data.provider

import com.coda.music.data.model.StreamResult

/**
 * Purpose: Contract for resolving a track ID to a playable stream URL.
 *
 * Returns [StreamResult] — never a raw String. All callers must handle all
 * three cases: Ready, Unavailable, Restricted.
 *
 * Dependencies: none (interface only)
 *
 * Public API: getStreamUrl(trackId: String): StreamResult
 *
 * Future TODOs:
 *   - When MediaSourceRouter is introduced (local file support), this interface
 *     becomes the arbitration point between NewPipe and local MediaStore sources.
 */
interface StreamProvider {
    suspend fun getStreamUrl(trackId: String): StreamResult
}
