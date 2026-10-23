package com.asadraza.streamflix.feature.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.asadraza.streamflix.core.common.util.ImageUrl
import com.asadraza.streamflix.core.model.movie.CastMember
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.model.movie.RelatedMovie
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.components.MovieCard
import com.asadraza.streamflix.core.ui.components.formWidth
import com.asadraza.streamflix.core.ui.text.toUiText
import com.asadraza.streamflix.core.ui.theme.Spacing
import java.util.Locale

/**
 * Movie detail screen: backdrop, key facts, cast and similar titles for one movie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    onPlayClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isInMyList by viewModel.isInMyList.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailEffect.NavigateBack -> onBackClick()
                is DetailEffect.NavigateToPlayer -> onPlayClick(effect.movieId)
                is DetailEffect.NavigateToDetail -> onMovieClick(effect.movieId)
                is DetailEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? DetailState.Success)?.movieDetail?.title
                            ?: stringResource(R.string.detail_loading),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(DetailEvent.OnBackClick) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back)
                        )
                    }
                },
                actions = {
                    (state as? DetailState.Success)?.let { success ->
                        IconButton(onClick = { context.shareMovie(success.movieDetail) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.detail_share))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val s = state) {
            is DetailState.Loading -> LoadingIndicator()
            is DetailState.Error -> {
                ErrorMessage(
                    message = s.errorType.toUiText().asString(),
                    onRetry = { viewModel.onEvent(DetailEvent.OnRetry) }
                )
            }
            is DetailState.Success -> {
                DetailContent(
                    movieDetail = s.movieDetail,
                    isInMyList = isInMyList,
                    onPlayTrailer = { viewModel.onEvent(DetailEvent.OnPlayTrailer) },
                    onToggleMyList = { viewModel.onEvent(DetailEvent.OnToggleMyList) },
                    onSimilarClick = { viewModel.onEvent(DetailEvent.OnSimilarClick(it)) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

private fun Context.shareMovie(detail: MovieDetail) {
    val text = "${detail.title} - https://www.themoviedb.org/movie/${detail.id}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, getString(R.string.detail_share_chooser)))
}

@Composable
internal fun DetailContent(
    movieDetail: MovieDetail,
    isInMyList: Boolean,
    onPlayTrailer: () -> Unit,
    onToggleMyList: () -> Unit,
    onSimilarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .formWidth(DETAIL_MAX_WIDTH)
            .verticalScroll(rememberScrollState())
    ) {
        Backdrop(movieDetail = movieDetail, onPlayTrailer = onPlayTrailer)

        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = movieDetail.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(Spacing.Small))

            movieDetail.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.Small))
            }

            Facts(movieDetail)

            Spacer(Modifier.height(Spacing.Medium))

            Text(
                text = movieDetail.genres.joinToString(" • ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.Medium))

            MyListButton(isInMyList = isInMyList, onClick = onToggleMyList)

            Spacer(Modifier.height(Spacing.Large))

            Text(
                text = stringResource(R.string.detail_overview),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(Spacing.Small))

            Text(
                text = movieDetail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            movieDetail.director?.let { director ->
                Spacer(Modifier.height(Spacing.Medium))
                Text(
                    text = stringResource(R.string.detail_director, director),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (movieDetail.cast.isNotEmpty()) {
            SectionTitle(stringResource(R.string.detail_cast))
            CastRow(movieDetail.cast)
        }

        if (movieDetail.similar.isNotEmpty()) {
            SectionTitle(stringResource(R.string.detail_similar))
            SimilarRow(movieDetail.similar, onSimilarClick)
        }

        if (movieDetail.productionCompanies.isNotEmpty()) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Text(
                    text = stringResource(R.string.detail_production_companies),
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

@Composable
private fun Backdrop(movieDetail: MovieDetail, onPlayTrailer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        AsyncImage(
            model = ImageUrl.backdrop(movieDetail.backdropPath),
            contentDescription = movieDetail.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for readability of the content below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))
                )
        )

        if (movieDetail.officialTrailer != null) {
            Button(onClick = onPlayTrailer, modifier = Modifier.align(Alignment.Center)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(Spacing.Small))
                Text(stringResource(R.string.detail_play_trailer))
            }
        }
    }
}

@Composable
private fun Facts(movieDetail: MovieDetail) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = stringResource(R.string.detail_rating),
            tint = Color(0xFFFFD700)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = String.format(Locale.getDefault(), "%.1f", movieDetail.voteAverage),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = " (${movieDetail.voteCount})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(Spacing.Medium))
        Text(text = movieDetail.formattedRuntime, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.width(Spacing.Medium))
        Text(text = movieDetail.releaseDate.take(4), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MyListButton(isInMyList: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(if (isInMyList) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(Spacing.Small))
        Text(stringResource(if (isInMyList) R.string.detail_in_list else R.string.detail_add_to_list))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(start = Spacing.Medium, top = Spacing.Medium, bottom = Spacing.Small)
            .semantics { heading() }
    )
}

@Composable
private fun CastRow(cast: List<CastMember>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        items(cast, key = { it.id }) { member ->
            Column(modifier = Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = ImageUrl.poster(member.profilePath, "w185"),
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.character,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SimilarRow(similar: List<RelatedMovie>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        items(similar, key = { it.id }) { movie ->
            MovieCard(
                posterUrl = ImageUrl.poster(movie.posterPath),
                title = movie.title,
                onClick = { onClick(movie.id) },
                modifier = Modifier.width(110.dp)
            )
        }
    }
}

private val DETAIL_MAX_WIDTH = 720.dp
