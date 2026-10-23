package com.asadraza.streamflix.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.asadraza.streamflix.core.common.util.ImageUrl
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.theme.Spacing
import com.asadraza.streamflix.feature.home.R

private const val HERO_BACKDROP_SIZE = "w1280"

/** Featured title at the top of Home: backdrop, title and the two primary actions. */
@Composable
internal fun HeroBanner(movie: Movie, onPlay: () -> Unit, onDetails: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        AsyncImage(
            model = ImageUrl.backdrop(movie.backdropPath ?: movie.posterPath, HERO_BACKDROP_SIZE),
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)))
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                Button(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.home_play))
                }
                Spacer(Modifier.width(Spacing.Small))
                OutlinedButton(onClick = onDetails) { Text(stringResource(R.string.home_details)) }
            }
        }
    }
}
