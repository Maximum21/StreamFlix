package com.asadraza.streamflix.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val CARD_SHAPE = RoundedCornerShape(8.dp)

/** Test tag of the grey card shown while a list item has not loaded yet. */
const val MOVIE_CARD_PLACEHOLDER_TAG = "movie_card_placeholder"

/**
 * Movie poster card. Titles without a poster (TMDB has none for many films) and posters that fail
 * to load show the title on the card, instead of an unexplained grey box.
 */
@Composable
fun MovieCard(posterUrl: String?, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // A new url gets a fresh chance: the card is reused when lazy lists recycle items
    var failed by remember(posterUrl) { mutableStateOf(false) }

    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f) // Standard movie poster ratio
            .clickable(onClick = onClick),
        shape = CARD_SHAPE,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (posterUrl == null || failed) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(8.dp)
                )
            }
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    onError = { failed = true },
                    modifier = Modifier.fillMaxSize().clip(CARD_SHAPE),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/** Takes the place of a poster whose data is still loading, so the grid keeps its shape. */
@Composable
fun MovieCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(MOVIE_CARD_PLACEHOLDER_TAG)
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(CARD_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
