package com.asadraza.streamflix.core.domain.usecase

import androidx.paging.PagingData
import com.asadraza.streamflix.core.domain.repository.MovieDiscoverRepository
import com.asadraza.streamflix.core.domain.usecase.search.DiscoverMoviesByGenreUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Test

class DiscoverMoviesByGenreUseCaseTest {
    private val repository = mockk<MovieDiscoverRepository>()
    private val useCase = DiscoverMoviesByGenreUseCase(repository)

    @Test
    fun `no genres means nothing to browse and the repository is not asked`() {
        useCase(emptySet())

        verify(exactly = 0) { repository.discoverMoviesByGenres(any()) }
    }

    @Test
    fun `selected genres are delegated to the repository`() {
        val expected: Flow<PagingData<Movie>> = flowOf(PagingData.empty())
        every { repository.discoverMoviesByGenres(setOf(28, 12)) } returns expected

        assertSame(expected, useCase(setOf(28, 12)))
    }
}
