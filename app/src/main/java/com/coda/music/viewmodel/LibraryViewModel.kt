package com.coda.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.UiError
import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import com.coda.music.ui.state.LibraryEvent
import com.coda.music.ui.state.LibraryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Manages UI state for LibraryScreen. getLikedSongs() returns an
 * empty list until toggleLike is implemented in Phase 2 (DataStore-backed).
 *
 * Dependencies: MusicRepository, NewPipeRepository (for mapToUiError),
 *               LibraryUiState, LibraryEvent, Hilt
 *
 * Public API:
 *   uiState: StateFlow<LibraryUiState>
 *   errors: Flow<UiError>
 *   onEvent(LibraryEvent)
 *
 * Future TODOs:
 *   - Wire toggleLike once DataStore persistence is added in Phase 2.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val newPipeRepository: NewPipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _errors = Channel<UiError>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: LibraryEvent) {
        // Navigation events handled by the screen via NavController.
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val liked = repository.getLikedSongs()
                _uiState.value = LibraryUiState.Success(liked)
            } catch (e: Exception) {
                val uiError = newPipeRepository.mapToUiError(e)
                if (_uiState.value is LibraryUiState.Loading) {
                    _uiState.value = LibraryUiState.Error(uiError.message)
                } else {
                    _errors.trySend(uiError)
                }
            }
        }
    }

    fun retry() = load()
}
