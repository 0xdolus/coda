package com.coda.music.data.model

/**
 * Purpose: Represents user-selectable audio stream quality tiers.
 * Stored in DataStore as the string name (e.g. "HIGH").
 * NewPipeStreamDataSource uses this to pick among available audio streams.
 */
enum class StreamQuality {
    LOW,    // ~48 kbps
    NORMAL, // ~128 kbps
    HIGH,   // ~160 kbps
    BEST    // highest available bitrate
}
