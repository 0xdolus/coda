package com.coda.music.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private fun Int.toTimestamp(): String {
    val m = this / 60
    val s = this % 60
    return "%d:%02d".format(m, s)
}

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
                var lastPlayingRotation by remember { mutableFloatStateOf(0f) }
                if (state.isPlaying) lastPlayingRotation = animatedRotation
                val rotation = if (state.isPlaying) animatedRotation else lastPlayingRotation

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = CodaDimens.ContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(CodaDimens.ItemSpacing))

                    // Vinyl disc — rotates while playing
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(CodaDimens.VinylSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .rotate(rotation)
                    ) {
                        // Album art — clipped to circle to fit vinyl
                        AsyncImage(
                            model = state.currentTrack.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
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

                    // Track info row — title + artist left, heart right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = state.currentTrack.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.currentTrack.artistName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(PlayerEvent.OnLikeToggle) }) {
                            Icon(
                                imageVector = if (state.isLiked) Icons.Filled.Favorite
                                              else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (state.isLiked) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

                    // Waveform progress bar
                    CodaProgressBar(
                        progressSeconds = state.progressSeconds,
                        durationSeconds = state.durationSeconds,
                        onSeek = { viewModel.onEvent(PlayerEvent.OnSeek(it)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Timestamps below waveform
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.progressSeconds.toTimestamp(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.durationSeconds.toTimestamp(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

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
