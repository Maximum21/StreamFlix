package com.asadraza.streamflix.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.asadraza.streamflix.core.common.util.ImageUrl
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.R
import com.asadraza.streamflix.core.ui.theme.Spacing

/**
 * Infinite-scrolling poster grid over Paging 3 data, shared by Home and Search.
 *
 * Pull-to-refresh calls [LazyPagingItems.refresh], which re-runs the RemoteMediator; the grid
 * keeps showing cached rows until the new page is stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginatedMovieGrid(
    movies: LazyPagingItems<Movie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = stringResource(R.string.grid_empty)
) {
    val loadState = movies.loadState
    val refreshState = loadState.refresh

    when {
        refreshState is LoadState.Loading && movies.itemCount == 0 -> LoadingIndicator()

        refreshState is LoadState.Error && movies.itemCount == 0 -> ErrorMessage(
            message = refreshState.error.localizedMessage ?: stringResource(R.string.grid_load_failed),
            onRetry = { movies.retry() }
        )

        refreshState is LoadState.NotLoading && movies.itemCount == 0 -> EmptyState(message = emptyMessage)

        else -> PullToRefreshBox(
            isRefreshing = refreshState is LoadState.Loading,
            onRefresh = { movies.refresh() },
            modifier = modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GRID_MIN_CELL),
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyGridState(),
                contentPadding = PaddingValues(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(
                    count = movies.itemCount,
                    key = movies.itemKey { it.id }
                ) { index ->
                    movies[index]?.let { movie ->
                        MovieCard(
                            posterUrl = ImageUrl.poster(movie.posterPath),
                            title = movie.title,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                }

                if (loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) { LoadingMoreIndicator() }
                }

                if (loadState.append is LoadState.Error) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingMoreError(onRetry = { movies.retry() })
                    }
                }
            }
        }
    }
}

private val GRID_MIN_CELL = 120.dp

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingMoreIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadingMoreError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onRetry) { Text(stringResource(R.string.grid_retry_more)) }
    }
}
