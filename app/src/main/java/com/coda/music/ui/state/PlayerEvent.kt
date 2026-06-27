package com.coda.music.ui.state

/**
 * Purpose: UI events for PlayerScreen.
 * Dependencies: none
 * Public API: sealed interface
 * Future TODOs: none
 */
sealed interface PlayerEvent {
    data object OnPlayPause : PlayerEvent
    data object OnNext : PlayerEvent
    data object OnPrevious : PlayerEvent
    data object OnShuffleToggle : PlayerEvent
    data object OnRepeatToggle : PlayerEvent
    data object OnLikeToggle : PlayerEvent
    data class OnSeek(val positionSeconds: Int) : PlayerEvent
    data object OnBackClick : PlayerEvent
}
