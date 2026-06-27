package com.coda.music.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.UiError
import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import com.coda.music.navigation.NavArguments
import com.coda.music.ui.state.ArtistEvent
import com.coda.music.ui.state.ArtistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Manages UI state for ArtistScreen. Loads artist info and songs
 * from MusicRepository using artistId from SavedStateHandle.
 *
 * Dependencies: MusicRepository, NewPipeRepository (for mapToUiError),
 *               SavedStateHandle, ArtistUiState, ArtistEvent, Hilt
 *
 * Public API:
 *   uiState: StateFlow<ArtistUiState>
 *   errors: Flow<UiError>
 *   onEvent(ArtistEvent)
 *
 * Future TODOs:
 *   - Load real playlists per artist once NewPipe supports it.
 */
@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val newPipeRepository: NewPipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[NavArguments.ARTIST_ID])

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private val _errors = Channel<UiError>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: ArtistEvent) {
        // Navigation events handled by the screen via NavController.
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            try {
                val artist = repository.getArtistInfo(artistId)
                val songs  = repository.getArtistSongs(artistId)
                _uiState.value = ArtistUiState.Success(
                    artist    = artist,
                    songs     = songs,
                    playlists = emptyList() // NewPipe does not return playlists per artist
                )
            } catch (e: Exception) {
                val uiError = newPipeRepository.mapToUiError(e)
                if (_uiState.value is ArtistUiState.Loading) {
                    _uiState.value = ArtistUiState.Error(uiError.message)
                } else {
                    _errors.trySend(uiError)
                }
            }
        }
    }

    fun retry() = load()
}
