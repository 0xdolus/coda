package com.coda.music.data.model

data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val imageUrl: String,
    val durationSeconds: Int
)
