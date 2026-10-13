package com.asadraza.streamflix.core.data.repository

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.database.datasource.LocalDataSource
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.network.datasource.RemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for MovieRepositoryImpl
 *
 * Ensure repository coordinates data sources correctly after implementing repository
 * It Mocks data sources and verify behavior
 */
class MovieRepositoryImplTest {

    private lateinit var remoteDataSource: RemoteDataSource
    private lateinit var localDataSource: LocalDataSource
    private lateinit var repository: MovieRepositoryImpl

    @Before
    fun setup() {
        remoteDataSource = mockk()
        localDataSource = mockk(relaxed = true)
        repository = MovieRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun `getMoviesByCategory emits cached data then fetches fresh data`() = runTest {
        // Arrange
        val category = MovieCategory.Trending
        val cachedEntities = listOf(createMovieEntity("1", category))
        val freshDtos = listOf(createMovieDto(2))

        coEvery { localDataSource.getMoviesByCategory(category.id) } returns flowOf(cachedEntities)
        coEvery { remoteDataSource.getTrendingMovies(1) } returns freshDtos

        // Act
        repository.getMoviesByCategory(category).test {
            // Assert
            // Should emit Loading
            assertTrue(awaitItem() is Result.Loading)

            // Should emit cached data
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            assertEquals(1, (cached as Result.Success).data.size)

            // Should emit fresh data
            val fresh = awaitItem()
            assertTrue(fresh is Result.Success)

            // Verify data was saved
            coVerify { localDataSource.insertMovies(any()) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshMovies clears cache and fetches fresh data`() = runTest {
        // Arrange
        val category = MovieCategory.Popular
        val freshDtos = listOf(createMovieDto(1))

        coEvery { remoteDataSource.getPopularMovies(1) } returns freshDtos

        // Act
        repository.refreshMovies(category)

        // Assert
        coVerify { localDataSource.deleteMoviesByCategory(category.id) }
        coVerify { remoteDataSource.getPopularMovies(1) }
        coVerify { localDataSource.insertMovies(any()) }
    }

    @Test
    fun `searchMovies always fetches from network`() = runTest {
        // Arrange
        val query = "action"
        val results = listOf(createMovieDto(1))

        coEvery { localDataSource.searchMovies(query) } returns flowOf(emptyList())
        coEvery { remoteDataSource.searchMovies(query, 1) } returns results

        // Act
        repository.searchMovies(query).test {
            // Assert
            assertTrue(awaitItem() is Result.Loading)

            val success = awaitItem()
            assertTrue(success is Result.Success)

            coVerify { remoteDataSource.searchMovies(query, 1) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getMovieDetails caches detail information`() = runTest {
        // Arrange
        val movieId = "123"
        val detailDto = createMovieDetailDto(movieId.toInt())

        coEvery { localDataSource.getMovieDetailById(movieId) } returns flowOf(null)
        coEvery { remoteDataSource.getMovieDetails(movieId.toInt()) } returns detailDto

        // Act
        repository.getMovieDetails(movieId).test {
            // Assert
            assertTrue(awaitItem() is Result.Loading)

            val success = awaitItem()
            assertTrue(success is Result.Success)

            coVerify { localDataSource.insertMovieDetail(any()) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    // Helper functions
    private fun createMovieEntity(id: String, category: MovieCategory) = MovieEntity(
        id = id,
        title = "Movie $id",
        description = "Description",
        backdropPath = null,
        posterPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 100,
        genreIds = "28,12",
        originalLanguage = "en",
        adult = false,
        video = false,
        category = category.id,
        cachedAt = System.currentTimeMillis()
    )

    private fun createMovieDto(id: Int) = MovieDto(
        id = id,
        title = "Movie $id",
        description = "Description",
        backdropPath = null,
        posterPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 100,
        genreIds = listOf(28, 12),
        originalLanguage = "en",
        adult = false,
        video = false
    )

    private fun createMovieDetailDto(id: Int) =
        com.asadraza.streamflix.core.network.model.MovieDetailDto(
            id = id,
            title = "Movie $id",
            description = "Description",
            backdropPath = null,
            posterPath = null,
            releaseDate = "2024-01-01",
            runtime = 120,
            voteAverage = 7.5,
            voteCount = 100,
            genres = emptyList(),
            productionCompanies = emptyList(),
            tagline = null,
            status = "Released",
            budget = 0,
            revenue = 0,
            originalLanguage = "en",
            spokenLanguages = emptyList(),
            videos = com.asadraza.streamflix.core.network.model.VideosResponse(emptyList())
        )
}