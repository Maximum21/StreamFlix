package com.asadraza.streamflix.core.data.repository

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDomain
import com.asadraza.streamflix.core.data.mapper.toEntity
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.VideosResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovieRepositoryImplTest {
    private lateinit var remote: IRemoteDataSource
    private lateinit var local: ILocalDataSource
    private lateinit var repository: MovieRepositoryImpl

    @Before
    fun setup() {
        remote = mockk()
        local = mockk(relaxed = true)
        repository = MovieRepositoryImpl(remote, local)
    }

    @Test
    fun `getMovieDetails fetches and caches when nothing is cached`() = runTest {
        val dto = detailDto(123)
        val saved = dto.toDomain().toEntity()
        every { local.getMovieDetailById("123") } returnsMany listOf(flowOf(null), flowOf(saved))
        coEvery { remote.getMovieDetails(123) } returns dto

        repository.getMovieDetails("123").test {
            assertTrue(awaitItem() is Result.Loading)
            assertEquals("Movie 123", (awaitItem() as Result.Success).data.title)
            awaitComplete()
        }
        coVerify { local.insertMovieDetail(any()) }
    }

    @Test
    fun `getMovieDetails serves the cache without hitting the network`() = runTest {
        val cached = detailDto(7).toDomain().toEntity()
        every { local.getMovieDetailById("7") } returns flowOf(cached)

        repository.getMovieDetails("7").test {
            assertTrue(awaitItem() is Result.Loading)
            assertTrue(awaitItem() is Result.Success)
            awaitComplete()
        }
        coVerify(exactly = 0) { remote.getMovieDetails(any()) }
    }

    private fun detailDto(id: Int) = MovieDetailDto(
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
        videos = VideosResponse(emptyList())
    )
}
