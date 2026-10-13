package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.network.model.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for MovieMapper
 *
 * It ensure correct mapping between layers After creating mapper
 * It Compares DTO and Domain fields
 */
class MovieMapperTest {

    @Test
    fun `movieDto toDomain maps all fields correctly`() {
        // Arrange
        val dto = MovieDto(
            id = 123,
            title = "Test Movie",
            description = "Test Description",
            backdropPath = "/backdrop.jpg",
            posterPath = "/poster.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 7.5,
            voteCount = 1000,
            genreIds = listOf(28, 12),
            originalLanguage = "en",
            adult = false,
            video = false
        )

        // Act
        val domain = dto.toDomain()

        // Assert
        assertEquals("123", domain.id)
        assertEquals("Test Movie", domain.title)
        assertEquals("Test Description", domain.description)
        assertEquals("/backdrop.jpg", domain.backdropPath)
        assertEquals("/poster.jpg", domain.posterPath)
        assertEquals("2024-01-01", domain.releaseDate)
        assertEquals(7.5, domain.voteAverage, 0.01)
        assertEquals(1000, domain.voteCount)
        assertEquals(listOf(28, 12), domain.genreIds)
        assertEquals("en", domain.originalLanguage)
        assertEquals(false, domain.adult)
        assertEquals(false, domain.video)
    }

    @Test
    fun `movie toEntity includes category and timestamp`() {
        // Arrange
        val movie = createTestMovie()
        val category = MovieCategory.Trending

        // Act
        val entity = movie.toEntity(category)

        // Assert
        assertEquals(movie.id, entity.id)
        assertEquals(movie.title, entity.title)
        assertEquals(category.id, entity.category)
        assert(entity.cachedAt > 0) { "Timestamp should be set" }
    }

    @Test
    fun `movieEntity toDomain preserves all data`() {
        // Arrange
        val movie = createTestMovie()
        val category = MovieCategory.Popular
        val entity = movie.toEntity(category)

        // Act
        val domainFromEntity = entity.toDomain()

        // Assert
        assertEquals(movie.id, domainFromEntity.id)
        assertEquals(movie.title, domainFromEntity.title)
        assertEquals(movie.description, domainFromEntity.description)
        assertEquals(movie.voteAverage, domainFromEntity.voteAverage, 0.01)
        assertEquals(movie.genreIds, domainFromEntity.genreIds)
    }

    @Test
    fun `genreIds correctly converts between List and String`() {
        // Arrange
        val genreIds = listOf(28, 12, 16)
        val movie = createTestMovie().copy(genreIds = genreIds)

        // Act
        val entity = movie.toEntity(MovieCategory.TopRated)
        val backToDomain = entity.toDomain()

        // Assert
        assertEquals("28,12,16", entity.genreIds)
        assertEquals(genreIds, backToDomain.genreIds)
    }

    private fun createTestMovie() = Movie(
        id = "1",
        title = "Test Movie",
        description = "Description",
        backdropPath = "/backdrop.jpg",
        posterPath = "/poster.jpg",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000,
        genreIds = listOf(28, 12),
        originalLanguage = "en",
        adult = false,
        video = false
    )
}