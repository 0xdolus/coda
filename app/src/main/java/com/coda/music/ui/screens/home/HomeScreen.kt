package com.coda.music.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coda.music.ui.components.ArtistAvatar
import com.coda.music.ui.components.SectionHeader
import com.coda.music.ui.components.SongRow
import com.coda.music.ui.components.TrendingVideoCard
import com.coda.music.ui.screens.ErrorScreen
import com.coda.music.ui.state.HomeUiState
import com.coda.music.ui.theme.CodaDimens
import com.coda.music.viewmodel.HomeViewModel
import androidx.compose.foundation.layout.Box

@Composable
fun HomeScreen(
    onArtistClick: (artistId: String) -> Unit,
    onTrackClick: (trackId: String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.errors.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = modifier
            )
        }
        is HomeUiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SectionHeader(title = "Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaDimens.ContentPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.artists) { artist ->
                        ArtistAvatar(
                            artist = artist,
                            onClick = onArtistClick
                        )
                    }
                }

                SectionHeader(title = "Trending")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaDimens.ContentPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.trendingVideos) { video ->
                        TrendingVideoCard(
                            video = video,
                            onClick = onTrackClick
                        )
                    }
                }

                SectionHeader(title = "Top Songs")
                state.topSongs.forEach { track ->
                    SongRow(
                        track = track,
                        onClick = onTrackClick
                    )
                }
            }
        }
    }
}
