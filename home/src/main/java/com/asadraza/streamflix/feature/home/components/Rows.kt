package com.asadraza.streamflix.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.asadraza.streamflix.core.common.util.ImageUrl
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.components.MovieCard
import com.asadraza.streamflix.core.ui.theme.Spacing
import com.asadraza.streamflix.feature.home.R
import kotlinx.coroutines.flow.Flow

private val POSTER_WIDTH = 120.dp
private val BACKDROP_WIDTH = 220.dp
private const val SKELETON_COUNT = 4
private const val BACKDROP_SIZE = "w780"

@Composable
internal fun RowTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            .semantics { heading() }
    )
}

/**
 * One catalogue row backed by its own paged feed. [refreshTick] lets the screen refresh every row
 * from a single pull-to-refresh: each increment re-runs the row's RemoteMediator.
 */
@Composable
internal fun CategoryRow(
    title: String,
    feed: Flow<androidx.paging.PagingData<Movie>>,
    refreshTick: Int,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies = feed.collectAsLazyPagingItems()
    LaunchedEffect(refreshTick) { if (refreshTick > 0) movies.refresh() }

    Column(modifier = modifier) {
        RowTitle(title)
        MovieRowContent(movies = movies, onMovieClick = onMovieClick)
    }
}

@Composable
private fun MovieRowContent(movies: LazyPagingItems<Movie>, onMovieClick: (String) -> Unit) {
    val refresh = movies.loadState.refresh
    when {
        movies.itemCount == 0 && refresh is LoadState.Error -> RowError(onRetry = { movies.retry() })
        movies.itemCount == 0 && refresh is LoadState.Loading -> SkeletonRow()
        else -> LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
                movies[index]?.let { movie ->
                    MovieCard(
                        posterUrl = ImageUrl.poster(movie.posterPath),
                        title = movie.title,
                        onClick = { onMovieClick(movie.id) },
                        modifier = Modifier.width(POSTER_WIDTH)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        repeat(SKELETON_COUNT) {
            Box(
                modifier = Modifier
                    .width(POSTER_WIDTH)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun RowError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.home_row_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.home_row_retry)) }
    }
}

@Composable
internal fun MyListRow(items: List<MyListItem>, onMovieClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        RowTitle(stringResource(R.string.home_my_list))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(items, key = { it.movieId }) { item ->
                MovieCard(
                    posterUrl = ImageUrl.poster(item.posterPath),
                    title = item.title,
                    onClick = { onMovieClick(item.movieId) },
                    modifier = Modifier.width(POSTER_WIDTH)
                )
            }
        }
    }
}

@Composable
internal fun ContinueWatchingRow(
    items: List<WatchedMovie>,
    onPlay: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RowTitle(stringResource(R.string.home_continue_watching))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(items, key = { it.movieId }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onPlay = { onPlay(item.movieId) },
                    onRemove = { onRemove(item.movieId) }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(item: WatchedMovie, onPlay: () -> Unit, onRemove: () -> Unit) {
    Column(modifier = Modifier.width(BACKDROP_WIDTH)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPlay)
        ) {
            AsyncImage(
                model = ImageUrl.backdrop(item.backdropPath ?: item.posterPath, BACKDROP_SIZE),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            )
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_remove_continue_watching, item.title),
                    tint = Color.White
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
