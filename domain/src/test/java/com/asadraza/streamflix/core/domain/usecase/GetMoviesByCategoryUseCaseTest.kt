package com.asadraza.streamflix.core.domain.usecase

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMoviesByCategoryUseCaseTest {

    private val repository: MovieRepository = mockk()
    private val useCase = GetMoviesByCategoryUseCase(repository)

    @Test
    fun `invoke returns movies from repository`() = runTest {
        // Arrange
        val category = MovieCategory.Trending
        val movies = listOf(createTestMovie())
        every { repository.getMoviesByCategory(category) } returns
                flowOf(Result.Success(movies))

        // Act
        val result = useCase(category)

        // Assert
        result.test {
            val emission = awaitItem()
            assertTrue(emission is Result.Success)
            assertEquals(movies, (emission as Result.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `invoke handles error from repository`() = runTest {
        // Arrange
        val category = MovieCategory.Popular
        //TODO work required
//        val exception = RuntimeException("Network error")
//        every { repository.getMoviesByCategory(category) } returns
//                flowOf(Result.Error(exception))
//
//        // Act
//        val result = useCase(category)
//
//        // Assert
//        result.test {
//            val emission = awaitItem()
//            assertTrue(emission is Result.Error)
//            assertEquals(exception, (emission as Result.Error))
//            awaitComplete()
//        }
    }

    private fun createTestMovie() = Movie(
        id = "1",
        title = "Test Movie",
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
}