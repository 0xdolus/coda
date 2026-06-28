package com.coda.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coda.music.ui.components.SectionHeader
import com.coda.music.ui.components.SongRow
import com.coda.music.ui.screens.ErrorScreen
import com.coda.music.ui.state.LibraryUiState
import com.coda.music.ui.theme.CodaDimens
import com.coda.music.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onTrackClick: (trackId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is LibraryUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LibraryUiState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = modifier
            )
        }
        is LibraryUiState.Success -> {
            Column(modifier = modifier.fillMaxSize()) {
                SectionHeader(title = "Liked Songs", modifier = Modifier.fillMaxWidth())
                if (state.likedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No liked songs yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(CodaDimens.ContentPadding)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(state.likedSongs) { track ->
                            SongRow(
                                track = track,
                                onClick = onTrackClick
                            )
                        }
                    }
                }
            }
        }
    }
}
