package com.coda.music.data.model

sealed class StreamResult {
    data class Ready(
        val url: String,
        val headers: Map<String, String> = emptyMap()
    ) : StreamResult()

    data object Unavailable : StreamResult()

    data class Restricted(val reason: String) : StreamResult()
}
