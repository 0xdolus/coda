package com.coda.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.coda.music.data.model.Playlist
import com.coda.music.ui.theme.CodaDimens

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: (playlistId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(CodaDimens.CardWidth)
            .clickable { onClick(playlist.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(CodaDimens.SpacingSmall)) {
            AsyncImage(
                model = playlist.imageUrl,
                contentDescription = playlist.name,
                modifier = Modifier
                    .size(CodaDimens.CardWidth)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(CodaDimens.SpacingXSmall))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
