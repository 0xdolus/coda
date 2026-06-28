package com.coda.music.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coda.music.ui.components.CodaSearchBar
import com.coda.music.ui.components.SongRow
import com.coda.music.ui.state.SearchEvent
import com.coda.music.ui.state.SearchUiState
import com.coda.music.ui.theme.CodaDimens
import com.coda.music.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onTrackClick: (trackId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        CodaSearchBar(
            query = query,
            onQueryChange = { q ->
                query = q
                viewModel.onEvent(SearchEvent.OnQueryChange(q))
            }
        )

        when (val state = uiState) {
            is SearchUiState.Idle -> {
                Column(modifier = Modifier.padding(CodaDimens.ContentPadding)) {
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SearchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = CodaDimens.ContentPadding)
                ) {
                    items(state.results) { track ->
                        SongRow(
                            track = track,
                            onClick = {
                                viewModel.onEvent(SearchEvent.OnTrackClick(it))
                                onTrackClick(it)
                            }
                        )
                    }
                }
            }
            is SearchUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results for \"${state.query}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(CodaDimens.ContentPadding)
                    )
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(CodaDimens.ContentPadding)
                    )
                }
            }
        }
    }
}
