package com.coda.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CodaColorScheme = darkColorScheme(
    primary              = CodaAccent,
    onPrimary            = CodaBackground,
    primaryContainer     = CodaCard,
    onPrimaryContainer   = CodaAccent,
    secondary            = CodaAccent,
    onSecondary          = CodaTextPrimary,
    secondaryContainer   = CodaCard,
    onSecondaryContainer = CodaAccent,
    background           = CodaBackground,
    onBackground         = CodaTextPrimary,
    surface              = CodaSurface,
    onSurface            = CodaTextPrimary,
    surfaceVariant       = CodaCard,
    onSurfaceVariant     = CodaTextSecondary,
    error                = CodaError,
    onError              = CodaTextPrimary
)

@Composable
fun CodaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CodaColorScheme,
        typography  = CodaTypography,
        shapes      = CodaShapes,
        content     = content
    )
}
