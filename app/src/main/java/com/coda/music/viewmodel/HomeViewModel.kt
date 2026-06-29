package com.coda.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.UiError
import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import com.coda.music.debug.LogBus
import com.coda.music.ui.state.HomeEvent
import com.coda.music.ui.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Manages UI state for HomeScreen. Loads artists, trending videos,
 * and top songs from MusicRepository on init. Exposes one-shot errors via
 * Channel<UiError> so Snackbars do not re-fire on recomposition.
 *
 * On network loss mid-session: emits a UiError Snackbar and retains the last
 * successful state. Only an initial-load failure escalates to Error state.
 *
 * Dependencies: MusicRepository, NewPipeRepository (for mapToUiError),
 *               HomeUiState, HomeEvent, Hilt
 *
 * Public API:
 *   uiState: StateFlow<HomeUiState>
 *   errors: Flow<UiError>
 *   onEvent(HomeEvent)
 *
 * Future TODOs:
 *   - Pull-to-refresh support.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val newPipeRepository: NewPipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _errors = Channel<UiError>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: HomeEvent) {
        // Navigation events are handled by the screen directly via NavController.
        // ViewModel receives them here if any non-navigation side effects are needed.
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            LogBus.d("CodaHome", "load() started")
            try {
                val artists       = repository.getArtists()
                val topSongs      = repository.getTopSongs()
                // TrendingVideo list derived from top songs for Phase 1 —
                // NewPipe does not expose a dedicated trending-video endpoint
                // separate from chart/search. Top songs are reused here as
                // trending content until a dedicated source is confirmed.
                val trendingVideos = topSongs.take(10).map { track ->
                    com.coda.music.data.model.TrendingVideo(
                        id           = track.id,
                        title        = track.title,
                        artist       = track.artistName,
                        thumbnailUrl = track.imageUrl
                    )
                }
                LogBus.d("CodaHome", "load() success — ${artists.size} artists, ${topSongs.size} songs")
                _uiState.value = HomeUiState.Success(
                    artists        = artists,
                    trendingVideos = trendingVideos,
                    topSongs       = topSongs
                )
            } catch (e: Exception) {
                LogBus.e("CodaHome", "load() failed: ${e::class.qualifiedName} - ${e.message}", e)

                val uiError = newPipeRepository.mapToUiError(e)
                if (_uiState.value is HomeUiState.Loading) {
                    // Initial load failure → critical ErrorScreen
                    _uiState.value = HomeUiState.Error(uiError.message)
                } else {
                    // Mid-session failure → Snackbar, retain last state
                    _errors.trySend(uiError)
                }
            }
        }
    }

    fun retry() = load()
}
