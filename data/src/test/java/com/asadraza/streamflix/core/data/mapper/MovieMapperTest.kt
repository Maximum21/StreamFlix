package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.network.model.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieMapperTest {
    private val dto = MovieDto(
        id = 123,
        title = "Test Movie",
        description = "Test Description",
        backdropPath = "/backdrop.jpg",
        posterPath = "/poster.jpg",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000,
        genreIds = listOf(28, 12, 16),
        originalLanguage = "en",
        adult = false,
        video = false
    )

    @Test
    fun `dto toDomain maps all fields`() {
        val domain = dto.toDomain()

        assertEquals("123", domain.id)
        assertEquals("Test Movie", domain.title)
        assertEquals("Test Description", domain.description)
        assertEquals("/backdrop.jpg", domain.backdropPath)
        assertEquals("/poster.jpg", domain.posterPath)
        assertEquals("2024-01-01", domain.releaseDate)
        assertEquals(7.5, domain.voteAverage, 0.01)
        assertEquals(1000, domain.voteCount)
        assertEquals(listOf(28, 12, 16), domain.genreIds)
        assertEquals("en", domain.originalLanguage)
    }

    @Test
    fun `dto toEntity stores genres as a comma separated string and stamps the cache time`() {
        val entity = dto.toEntity(cachedAt = 5L)

        assertEquals("123", entity.id)
        assertEquals("28,12,16", entity.genreIds)
        assertEquals(5L, entity.cachedAt)
    }

    @Test
    fun `entity toDomain round trips the dto data`() {
        val domain = dto.toEntity().toDomain()

        assertEquals(dto.toDomain(), domain)
    }

    @Test
    fun `toEntity defaults the cache time to now`() {
        assertTrue(dto.toEntity().cachedAt > 0)
    }
}
