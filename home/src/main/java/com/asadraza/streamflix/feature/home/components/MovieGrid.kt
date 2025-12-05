package com.asadraza.streamflix.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.components.MovieCard
import com.asadraza.streamflix.core.ui.theme.Spacing

/**
 * Grid of movies
 *
 * Reusable grid component to Displaying list of movies
 * In home screen, search results
 * Uses LazyVerticalGrid for performance
 */
@Composable
fun MovieGrid(
    movies: List<Movie>,
    isLoading: Boolean,
    error: String?,
    onMovieClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading && movies.isEmpty() -> {
            LoadingIndicator()
        }

        error != null && movies.isEmpty() -> {
            ErrorMessage(
                message = error,
                onRetry = onRetry
            )
        }

        movies.isNotEmpty() -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(
                    items = movies,
                    key = { it.id }
                ) { movie ->
                    MovieCard(
                        posterUrl = movie.fullPosterUrl,
                        title = movie.title,
                        onClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }
}