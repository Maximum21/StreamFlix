package com.asadraza.streamflix.feature.home.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.components.MovieCard
import com.asadraza.streamflix.core.ui.theme.Spacing

/**
 * Paginated Grid of movies using Paging 3
 *
 * Features:
 * - Infinite scrolling with automatic page loading
 * - Loading states (initial, append, prepend)
 * - Error handling with retry capability
 * - Empty state handling
 *
 * Uses LazyVerticalGrid with Paging 3 LazyPagingItems
 */
@Composable
fun PaginatedMovieGrid(
    movies: LazyPagingItems<Movie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No movies found"
) {
    val loadState = movies.loadState
    val gridState = rememberLazyGridState()
    when {
        // Initial Loading
        loadState.refresh is LoadState.Loading && movies.itemCount == 0 -> {
            LoadingIndicator()
        }

        // Initial Error
        loadState.refresh is LoadState.Error && movies.itemCount == 0 -> {
            val error = loadState.refresh as LoadState.Error
            ErrorMessage(
                message = error.error.localizedMessage ?: "Failed to load movies",
                onRetry = { movies.retry() }
            )
        }

        // Empty State (after successful load)
        loadState.refresh is LoadState.NotLoading && movies.itemCount == 0 -> {
            EmptyState(message = emptyMessage)
        }

        // Content with items
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                // Movie items
                items(
                    count = movies.itemCount,
                    key = { index -> movies.peek(index)?.id ?: "placeholder_$index" }
                ) { index ->
                    movies[index]?.let { movie ->
                        MovieCard(
                            posterUrl = movie.fullPosterUrl,
                            title = movie.title,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                }

                // Loading more indicator at bottom
                if (loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(2) }) {
                        LoadingMoreIndicator()
                    }
                }

                // Error loading more with retry
                if (loadState.append is LoadState.Error) {
                    item(span = { GridItemSpan(2) }) {
                        LoadingMoreError(
                            error = (loadState.append as LoadState.Error).error,
                            onRetry = { movies.retry() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state component
 */
@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Loading indicator for pagination (append loading)
 */
@Composable
private fun LoadingMoreIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state for pagination failures with retry
 */
@Composable
private fun LoadingMoreError(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onRetry) {
            Text("Retry loading more")
        }
    }
}