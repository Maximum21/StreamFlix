package com.asadraza.streamflix.feature.home

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryUseCase
import com.asadraza.streamflix.core.domain.usecase.home.RefreshMoviesUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for HomeViewModel
 *
 * Verify state management and event handling after implementing ViewModel
 * Mock UseCases, test state changes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var getMoviesUseCase: GetMoviesByCategoryUseCase
    private lateinit var refreshMoviesUseCase: RefreshMoviesUseCase
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMoviesUseCase = mockk()
        refreshMoviesUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Arrange
        val movies = listOf(createTestMovie())
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Success(movies))

        // Act
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertFalse(state.isOffline)
        assertEquals(MovieCategory.Trending, state.selectedCategory)
    }

    @Test
    fun `loading state updates correctly`() = runTest {
        // Arrange
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Loading())

        // Act
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)

        // Assert
        viewModel.state.test {
            val state = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            val loadingState = awaitItem()
            assertTrue(loadingState.movieCategories.values.any { it.isLoading })
        }
    }

    @Test
    fun `success state contains movies`() = runTest {
        // Arrange
        val movies = listOf(createTestMovie("1"), createTestMovie("2"))
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Success(movies))

        // Act
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        val trendingState = state.movieCategories[MovieCategory.Trending]
        assertEquals(2, trendingState?.movies?.size)
        assertFalse(trendingState?.isLoading ?: true)
    }

    @Test
    fun `error state shows error message`() = runTest {
        // Arrange
        //TODO network exception check
        val exception = RuntimeException("Network error")
//        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Error())

        // Act
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        val trendingState = state.movieCategories[MovieCategory.Trending]
        assertEquals("Network error", trendingState?.error)
    }

    @Test
    fun `OnMovieClick emits NavigateToDetail effect`() = runTest {
        // Arrange
        val movieId = "123"
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Success(emptyList()))
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)

        // Act
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.OnMovieClick(movieId))

            // Assert
            val effect = awaitItem()
            assertTrue(effect is HomeEffect.NavigateToDetail)
            assertEquals(movieId, (effect as HomeEffect.NavigateToDetail).movieId)
        }
    }

    @Test
    fun `OnRefresh calls refreshMoviesUseCase`() = runTest {
        // Arrange
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Success(emptyList()))
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(HomeEvent.OnRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { refreshMoviesUseCase(any()) }
    }

    @Test
    fun `OnCategorySelect updates selectedCategory`() = runTest {
        // Arrange
        coEvery { getMoviesUseCase(any()) } returns flowOf(Result.Success(emptyList()))
        viewModel = HomeViewModel(getMoviesUseCase, refreshMoviesUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onEvent(HomeEvent.OnCategorySelect(MovieCategory.Popular))

        // Assert
        assertEquals(MovieCategory.Popular, viewModel.state.value.selectedCategory)
    }

    private fun createTestMovie(id: String = "1") = Movie(
        id = id,
        title = "Test Movie $id",
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