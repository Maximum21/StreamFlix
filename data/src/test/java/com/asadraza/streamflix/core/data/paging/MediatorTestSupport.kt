package com.asadraza.streamflix.core.data.paging

import android.content.Context
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.asadraza.streamflix.core.database.StreamFlixDatabase
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.datasource.LocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieDto
import com.asadraza.streamflix.core.network.model.MovieListResponse
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class InMemoryDatabaseRule : TestWatcher() {
    lateinit var db: StreamFlixDatabase
        private set
    lateinit var local: ILocalDataSource
        private set

    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StreamFlixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        local = LocalDataSource(db, db.movieDao(), db.movieDetailDao(), db.remoteKeyDao())
    }

    override fun finished(description: Description) {
        db.close()
    }
}

fun emptyPagingState() = PagingState<Int, MovieEntity>(
    pages = emptyList(),
    anchorPosition = null,
    config = PagingConfig(pageSize = 20),
    leadingPlaceholderCount = 0
)

suspend fun PagingSource<Int, MovieEntity>.ids(): List<String> {
    val result = load(PagingSource.LoadParams.Refresh(key = null, loadSize = 200, placeholdersEnabled = false))
    return (result as PagingSource.LoadResult.Page).data.map { it.id }
}

fun movieDto(id: Int) = MovieDto(
    id = id,
    title = "Movie $id",
    description = "Description",
    backdropPath = null,
    posterPath = null,
    releaseDate = "2024-01-01",
    voteAverage = 7.5,
    voteCount = 100,
    genreIds = listOf(28),
    originalLanguage = "en",
    adult = false,
    video = false
)

fun page(page: Int, totalPages: Int, ids: IntRange) = MovieListResponse(
    page = page,
    results = ids.map(::movieDto),
    totalPages = totalPages,
    totalResults = totalPages * 20
)

fun emptyPage(page: Int, totalPages: Int) = MovieListResponse(page, emptyList(), totalPages, 0)

/** Serves canned pages; any other request fails with [failure] when set. */
class FakeRemoteDataSource(
    var pages: Map<Int, MovieListResponse> = emptyMap(),
    var failure: Throwable? = null
) : IRemoteDataSource {
    val requestedPages = mutableListOf<Int>()
    val requestedQueries = mutableListOf<String>()

    private fun serve(page: Int): MovieListResponse {
        requestedPages += page
        failure?.let { throw it }
        return pages.getValue(page)
    }

    override suspend fun getTrendingMoviesPaginated(page: Int) = serve(page)

    override suspend fun getPopularMoviesPaginated(page: Int) = serve(page)

    override suspend fun getTopRatedMoviesPaginated(page: Int) = serve(page)

    override suspend fun getUpcomingMoviesPaginated(page: Int) = serve(page)

    override suspend fun getNowPlayingMoviesPaginated(page: Int) = serve(page)

    override suspend fun searchMoviesPaginated(query: String, page: Int): MovieListResponse {
        requestedQueries += query
        return serve(page)
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDetailDto = error("not used")
}
