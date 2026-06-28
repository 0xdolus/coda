package com.coda.music

import androidx.compose.runtime.compositionLocalOf
import com.coda.music.player.PlayerController

object LocalProviders {
    val LocalPlayerController = compositionLocalOf<PlayerController> {
        error("No PlayerController provided")
    }
}
