package com.coda.music.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Purpose: Foreground service that owns the ExoPlayer instance and MediaSession.
 *          Running as a MediaSessionService keeps playback alive when the app
 *          is backgrounded or the screen is off. Media3 automatically posts and
 *          manages the playback notification while the service is active.
 *
 * Dependencies: ExoPlayer (media3-exoplayer), MediaSession (media3-session), Hilt
 *
 * Public API: none — accessed exclusively through MediaController in PlayerController.
 *
 * Future TODOs:
 *   - Custom MediaNotificationProvider for Coda branding / accent colour
 *   - Audio focus request customisation beyond ExoPlayer defaults
 *   - Sleep timer: stopSelf() after a delay posted here
 */
@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
