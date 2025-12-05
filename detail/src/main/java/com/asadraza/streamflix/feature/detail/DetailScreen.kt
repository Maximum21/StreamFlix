package com.asadraza.streamflix.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Movie Detail Screen
 *
 * Shows comprehensive movie information
 * User taps on movie from home/search
 * Scrollable detail view with backdrop, poster, info
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DetailEffect.NavigateBack -> onBackClick()
                is DetailEffect.NavigateToDetail -> onMovieClick(effect.movieId)
                is DetailEffect.NavigateToPlayer -> onPlayClick(effect.movieId)
                is DetailEffect.ShowToast -> {
                    // Show toast
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.movieDetail?.title ?: "Loading...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(DetailEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null && state.movieDetail == null -> {
                ErrorMessage(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.onEvent(DetailEvent.OnRetry) }
                )
            }
            state.movieDetail != null -> {
                DetailContent(
                    movieDetail = state.movieDetail!!,
                    onPlayTrailer = { viewModel.onEvent(DetailEvent.OnPlayTrailer) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Detail screen content
 * Separate composable for complex layout
 */
@Composable
private fun DetailContent(
    movieDetail: MovieDetail,
    onPlayTrailer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Backdrop with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = movieDetail.fullBackdropUrl,
                contentDescription = movieDetail.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Play trailer button
            if (movieDetail.officialTrailer != null) {
                Button(
                    onClick = onPlayTrailer,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Spacing.Small))
                    Text("Play Trailer")
                }
            }
        }

        // Movie info
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            // Title
            Text(
                text = movieDetail.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(Spacing.Small))

            // Tagline
            movieDetail.tagline?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.Small))
            }

            // Rating, runtime, release date
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFD700)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.getDefault(),"%.1f", movieDetail.voteAverage),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.width(Spacing.Medium))

                Text(
                    text = movieDetail.formattedRuntime,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.width(Spacing.Medium))

                Text(
                    text = movieDetail.releaseDate.take(4), // Year
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(Spacing.Medium))

            // Genres
            Text(
                text = movieDetail.genres.joinToString(" • ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.Large))

            // Description
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(Spacing.Small))

            Text(
                text = movieDetail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.Large))

            // Production companies
            if (movieDetail.productionCompanies.isNotEmpty()) {
                Text(
                    text = "Production Companies",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(Spacing.Small))

                Text(
                    text = movieDetail.productionCompanies.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}