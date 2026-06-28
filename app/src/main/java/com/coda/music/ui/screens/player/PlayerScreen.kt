package com.coda.music.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.coda.music.ui.components.CodaProgressBar
import com.coda.music.ui.components.PlaybackControls
import com.coda.music.ui.screens.ErrorScreen
import com.coda.music.ui.state.PlayerEvent
import com.coda.music.ui.state.PlayerUiState
import com.coda.music.ui.theme.CodaDimens
import com.coda.music.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PlayerUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = viewModel::retry
                )
            }
            is PlayerUiState.TrackUnavailable -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Track unavailable\n${state.reason}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(CodaDimens.ContentPadding)
                    )
                }
            }
            is PlayerUiState.Success -> {
                val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
                val animatedRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "vinyl_rotation"
                )
                // Freeze the vinyl at whatever angle it was at when playback
                // pauses, instead of continuing to tick — the animation itself
                // never stops, so we latch the last value while paused and
                // only read the live animated value while playing.
                var lastPlayingRotation by remember { mutableFloatStateOf(0f) }
                if (state.isPlaying) lastPlayingRotation = animatedRotation
                val rotation = if (state.isPlaying) animatedRotation else lastPlayingRotation

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = CodaDimens.ContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

                    // Spinning vinyl — rotates while playing, holds still when paused
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(CodaDimens.VinylSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .rotate(rotation)
                    ) {
                        AsyncImage(
                            model = state.currentTrack.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(CodaDimens.VinylArtInset)
                                .clip(CircleShape)
                        )
                        // Centre hole
                        Box(
                            modifier = Modifier
                                .size(CodaDimens.VinylCentreHole)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

                    // Track info + like
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 48.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = state.currentTrack.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.currentTrack.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(PlayerEvent.OnLikeToggle) },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (state.isLiked) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CodaDimens.ItemSpacing))

                    // Waveform progress bar
                    CodaProgressBar(
                        progressSeconds = state.progressSeconds,
                        durationSeconds = state.durationSeconds,
                        onSeek = { viewModel.onEvent(PlayerEvent.OnSeek(it)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(CodaDimens.ItemSpacing))

                    // Playback controls
                    PlaybackControls(
                        isPlaying = state.isPlaying,
                        shuffleEnabled = state.shuffleEnabled,
                        repeatEnabled = state.repeatEnabled,
                        onPlayPause = { viewModel.onEvent(PlayerEvent.OnPlayPause) },
                        onNext = { viewModel.onEvent(PlayerEvent.OnNext) },
                        onPrevious = { viewModel.onEvent(PlayerEvent.OnPrevious) },
                        onShuffleToggle = { viewModel.onEvent(PlayerEvent.OnShuffleToggle) },
                        onRepeatToggle = { viewModel.onEvent(PlayerEvent.OnRepeatToggle) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
