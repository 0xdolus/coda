package com.coda.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private const val BAR_COUNT = 52

/**
 * Waveform progress bar: 52 sine-shaped bars.
 * Elapsed bars in accent (#cf8089 → primary), remaining in rgba(255,255,255,0.13).
 * Seekable on tap.
 */
@Composable
fun CodaProgressBar(
    progressSeconds: Int,
    durationSeconds: Int,
    onSeek: (positionSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val remaining = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)

    val fraction = if (durationSeconds > 0) {
        (progressSeconds.toFloat() / durationSeconds).coerceIn(0f, 1f)
    } else 0f

    val heights = remember {
        FloatArray(BAR_COUNT) { i ->
            val normalized = i.toFloat() / BAR_COUNT
            0.3f + 0.7f * sin(normalized * Math.PI * 3).toFloat().coerceAtLeast(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(durationSeconds) {
                detectTapGestures { offset ->
                    val seekFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((seekFraction * durationSeconds).toInt())
                }
            }
    ) {
        val barWidth = size.width / (BAR_COUNT * 1.6f)
        val gap = size.width / BAR_COUNT

        heights.forEachIndexed { index, heightFraction ->
            val barHeight = size.height * heightFraction.coerceIn(0.1f, 1f)
            val x = index * gap
            val top = (size.height - barHeight) / 2f
            val color: Color = if (index.toFloat() / BAR_COUNT <= fraction) accent else remaining

            drawRect(
                color = color,
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}
