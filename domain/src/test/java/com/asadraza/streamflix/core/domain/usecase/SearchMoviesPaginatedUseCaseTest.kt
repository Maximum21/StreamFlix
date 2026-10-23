package com.asadraza.streamflix.core.domain.usecase

import androidx.paging.PagingData
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.domain.usecase.search.SearchMoviesPaginatedUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Test

class SearchMoviesPaginatedUseCaseTest {
    private val repository: MovieSearchRepository = mockk()
    private val useCase = SearchMoviesPaginatedUseCase(repository)

    @Test
    fun `short query does not reach the repository`() {
        useCase("ab")
        useCase("  a ")

        verify(exactly = 0) { repository.searchMoviesPaginated(any()) }
    }

    @Test
    fun `valid query is trimmed and delegated to the repository`() {
        val expected: Flow<PagingData<Movie>> = flowOf(PagingData.empty())
        every { repository.searchMoviesPaginated("batman") } returns expected

        assertSame(expected, useCase("  batman "))
    }
}
