package com.coda.music.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coda.music.debug.LogBus
import com.coda.music.debug.LogBus
import com.coda.music.data.model.StreamQuality
import com.coda.music.ui.theme.CodaDimens
import com.coda.music.viewmodel.SettingsViewModel

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val quality by viewModel.streamQuality.collectAsStateWithLifecycle()
    val shuffle by viewModel.defaultShuffle.collectAsStateWithLifecycle()
    val repeat by viewModel.defaultRepeat.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CodaDimens.ContentPadding)
    ) {
        Text(
            text = "Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

        // — Audio —
        SectionLabel("Audio")

        Text(
            text = "Stream Quality",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = "Applied immediately to the current track",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        StreamQuality.entries.forEach { option ->
            QualityRow(
                label = when (option) {
                    StreamQuality.LOW    -> "Low  (~48 kbps)"
                    StreamQuality.NORMAL -> "Normal  (~128 kbps)"
                    StreamQuality.HIGH   -> "High  (~160 kbps)"
                    StreamQuality.BEST   -> "Best available"
                },
                selected = quality == option,
                onClick = { viewModel.setStreamQuality(option) }
            )
        }

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

        // — Playback —
        SectionLabel("Playback")

        ToggleRow(
            label = "Shuffle by default",
            checked = shuffle,
            onCheckedChange = viewModel::setDefaultShuffle
        )
        ToggleRow(
            label = "Repeat by default",
            checked = repeat,
            onCheckedChange = viewModel::setDefaultRepeat
        )

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

        // — Debug Logs —
        SectionLabel("Debug Logs")

        val logEntries by LogBus.entries.collectAsStateWithLifecycle()

        Text(
            text = "In-app log buffer — works without ADB/root, since logcat " +
                   "is restricted to system apps on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { LogBus.clear() }) {
            Text("Clear logs")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logEntries.isEmpty()) {
            Text(
                text = "No logs yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logEntries.asReversed().forEach { entry ->
                Text(
                    text = "[${entry.timestamp}] ${entry.tag}: ${entry.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.isError)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

        // — Debug Logs —
        SectionLabel("Debug Logs")

        val logEntries by LogBus.entries.collectAsStateWithLifecycle()

        Text(
            text = "In-app log buffer — works without ADB/root, since logcat " +
                   "is restricted to system apps on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { LogBus.clear() }) {
            Text("Clear logs")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logEntries.isEmpty()) {
            Text(
                text = "No logs yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logEntries.asReversed().forEach { entry ->
                Text(
                    text = "[${entry.timestamp}] ${entry.tag}: ${entry.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.isError)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))

        // — About —
        SectionLabel("About")

        InfoRow(label = "Version", value = "1.0.0")
        InfoRow(label = "Audio source", value = "NewPipeExtractor 0.26.3")

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "License notice",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This app uses NewPipeExtractor (GPL-3.0). " +
                   "This build is for personal use only and is not distributed. " +
                   "Redistribution requires GPL-3.0 compliance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(CodaDimens.SectionSpacing))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun QualityRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
