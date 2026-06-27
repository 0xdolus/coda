package com.coda.music.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.StreamResult
import com.coda.music.data.model.UiError
import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import com.coda.music.navigation.NavArguments
import com.coda.music.player.PlayerController
import com.coda.music.ui.state.PlayerEvent
import com.coda.music.ui.state.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Manages UI state for PlayerScreen. Observes PlayerController for
 * playback state and additionally injects MusicRepository for things
 * PlayerController doesn't own (e.g. isLiked). Does NOT hold its own copy
 * of playback state — it maps from PlayerController.playbackState.
 *
 * On init, loads the track for trackId from MusicRepository, then begins
 * playback via PlayerController. All three StreamResult cases are handled:
 *   Ready       → playback starts via PlayerController.play()
 *   Unavailable → PlayerUiState.TrackUnavailable
 *   Restricted  → PlayerUiState.TrackUnavailable(reason)
 *
 * Dependencies: MusicRepository, NewPipeRepository (mapToUiError),
 *               PlayerController, SavedStateHandle, PlayerUiState,
 *               PlayerEvent, Hilt
 *
 * Public API:
 *   uiState: StateFlow<PlayerUiState>
 *   errors: Flow<UiError>
 *   onEvent(PlayerEvent)
 *
 * Future TODOs:
 *   - Wire isLiked from DataStore once toggleLike is implemented in Phase 2.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val newPipeRepository: NewPipeRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val trackId: String = checkNotNull(savedStateHandle[NavArguments.TRACK_ID])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _errors = Channel<UiError>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        load()
        observePlaybackState()
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.OnPlayPause     -> playerController.togglePlayPause()
            is PlayerEvent.OnNext          -> playerController.skipToNext()
            is PlayerEvent.OnPrevious      -> playerController.skipToPrevious()
            is PlayerEvent.OnShuffleToggle -> playerController.toggleShuffle()
            is PlayerEvent.OnRepeatToggle  -> playerController.toggleRepeat()
            is PlayerEvent.OnSeek          -> playerController.seekTo(event.positionSeconds)
            is PlayerEvent.OnLikeToggle    -> { /* Phase 2 — DataStore toggleLike */ }
            is PlayerEvent.OnBackClick     -> { /* handled by screen via NavController */ }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val track = repository.getTrack(trackId)
                // Resolve stream and start playback via PlayerController.
                // PlayerController.play() handles all three StreamResult cases internally
                // and updates PlaybackState accordingly.
                playerController.play(track)
            } catch (e: Exception) {
                val uiError = newPipeRepository.mapToUiError(e)
                _uiState.value = PlayerUiState.Error(uiError.message)
            }
        }
    }

    /**
     * Observes PlayerController.playbackState and maps it to PlayerUiState.
     * This is the only place playback state enters the UI layer — never
     * duplicated in any other ViewModel or screen.
     */
    private fun observePlaybackState() {
        viewModelScope.launch {
            playerController.playbackState.collect { playback ->
                val current = playback.currentTrack ?: return@collect
                // isLiked is always false until Phase 2 DataStore integration
                _uiState.value = PlayerUiState.Success(
                    currentTrack    = current,
                    isPlaying       = playback.isPlaying,
                    isLiked         = false,
                    progressSeconds = playback.progressSeconds,
                    durationSeconds = playback.durationSeconds,
                    shuffleEnabled  = playback.shuffleEnabled,
                    repeatEnabled   = playback.repeatEnabled
                )
            }
        }
    }

    fun retry() = load()
}
