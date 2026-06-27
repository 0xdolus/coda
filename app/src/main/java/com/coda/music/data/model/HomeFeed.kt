package com.coda.music.data.model

data class HomeFeed(
    val artists: List<Artist>,
    val trendingVideos: List<TrendingVideo>,
    val topSongs: List<Track>
)
