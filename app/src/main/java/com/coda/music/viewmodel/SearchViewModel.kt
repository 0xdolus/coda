package com.coda.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coda.music.data.model.UiError
import com.coda.music.data.repository.MusicRepository
import com.coda.music.data.repository.NewPipeRepository
import com.coda.music.ui.state.SearchEvent
import com.coda.music.ui.state.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Purpose: Manages UI state for SearchScreen. Applies debounce(300) +
 * distinctUntilChanged to the query before issuing a NewPipe call — text
 * input itself stays instant; only the network call is debounced.
 *
 * SearchUiState.Empty is its own state for zero-result queries — never
 * represented as Success(query, emptyList()).
 *
 * Dependencies: MusicRepository, NewPipeRepository (for mapToUiError),
 *               SearchUiState, SearchEvent, Hilt
 *
 * Public API:
 *   uiState: StateFlow<SearchUiState>
 *   errors: Flow<UiError>
 *   onEvent(SearchEvent)
 *
 * Future TODOs:
 *   - Add search history persistence via DataStore.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val newPipeRepository: NewPipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _errors = Channel<UiError>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    private var searchJob: Job? = null
    private var lastQuery: String = ""

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange    -> onQueryChange(event.query)
            is SearchEvent.OnTrackClick     -> { /* handled by screen */ }
            is SearchEvent.OnCategoryClick  -> { /* handled by screen */ }
        }
    }

    private fun onQueryChange(query: String) {
        if (query.isBlank()) {
            searchJob?.cancel()
            lastQuery = ""
            _uiState.value = SearchUiState.Idle()
            return
        }

        // distinctUntilChanged — skip if same query
        if (query == lastQuery) return

        // debounce(300)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            lastQuery = query
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.search(query)
                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Empty(query)
                } else {
                    SearchUiState.Success(query, results)
                }
            } catch (e: Exception) {
                val uiError = newPipeRepository.mapToUiError(e)
                _uiState.value = SearchUiState.Error(uiError.message)
            }
        }
    }

    fun retry() {
        if (lastQuery.isNotBlank()) onQueryChange(lastQuery)
    }
}
