package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.util.networkResult
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.network.model.CastDto
import com.asadraza.streamflix.core.network.model.CreditsDto
import com.asadraza.streamflix.core.network.model.CrewDto
import com.asadraza.streamflix.core.network.model.GenreDto
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.MovieDto
import com.asadraza.streamflix.core.network.model.ProductionCompanyDto
import com.asadraza.streamflix.core.network.model.SimilarResponse
import com.asadraza.streamflix.core.network.model.SpokenLanguageDto
import com.asadraza.streamflix.core.network.model.VideoDto
import com.asadraza.streamflix.core.network.model.VideosResponse
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDetailMapperTest {
    private val dto = MovieDetailDto(
        id = 42,
        title = "Title",
        description = "Overview",
        backdropPath = "/b.jpg",
        posterPath = null,
        releaseDate = "2024-05-01",
        runtime = 90,
        voteAverage = 8.1,
        voteCount = 10,
        genres = listOf(GenreDto(28, "Action")),
        productionCompanies = listOf(ProductionCompanyDto(1, "Studio", "/l.png", "US")),
        tagline = "Tag",
        status = "Released",
        budget = 5,
        revenue = 6,
        originalLanguage = "en",
        spokenLanguages = listOf(SpokenLanguageDto("en", "English")),
        videos = VideosResponse(listOf(VideoDto("v1", "key1", "Trailer 1", "YouTube", "Trailer", true)))
    )

    private fun movieDto(id: Int) = MovieDto(
        id = id,
        title = "Similar $id",
        description = "",
        backdropPath = null,
        posterPath = "/s$id.jpg",
        releaseDate = "",
        voteAverage = 1.0,
        voteCount = 1,
        genreIds = emptyList(),
        originalLanguage = "en",
        adult = false,
        video = false
    )

    @Test
    fun `credits and similar titles are mapped and capped`() {
        val enriched = dto.copy(
            credits = CreditsDto(
                cast = List(20) { CastDto(it, "Actor $it", "Role $it", "/a$it.jpg") },
                crew = listOf(CrewDto(1, "Editor Ed", "Editor"), CrewDto(2, "Dana Director", "Director"))
            ),
            similar = SimilarResponse(List(30) { movieDto(it) })
        )

        val detail = enriched.toDomain()

        assertEquals(15, detail.cast.size)
        assertEquals("Actor 0", detail.cast.first().name)
        assertEquals("Dana Director", detail.director)
        assertEquals(12, detail.similar.size)
        assertEquals("0", detail.similar.first().id)
        assertEquals("/s0.jpg", detail.similar.first().posterPath)
    }

    @Test
    fun `a response without credits maps to empty lists and no director`() {
        val detail = dto.toDomain()

        assertTrue(detail.cast.isEmpty())
        assertTrue(detail.similar.isEmpty())
        assertEquals(null, detail.director)
    }

    @Test
    fun `the cached entity round-trips the enriched fields`() {
        val detail = dto.copy(
            credits = CreditsDto(listOf(CastDto(5, "Actor", "Role", null)), listOf(CrewDto(2, "Dana", "Director"))),
            similar = SimilarResponse(listOf(movieDto(9)))
        ).toDomain()

        assertEquals(detail, detail.toEntity().toDomain())
    }

    @Test
    fun `detail dto maps every field`() {
        val detail = dto.toDomain()

        assertEquals("42", detail.id)
        assertEquals("Overview", detail.description)
        assertEquals(Genre(28, "Action"), detail.genres.single())
        assertEquals("Studio", detail.productionCompanies.single().name)
        assertEquals("English", detail.spokenLanguages.single().name)
        assertEquals("key1", detail.officialTrailer?.key)
        assertEquals("1h 30m", detail.formattedRuntime)
    }

    @Test
    fun `networkResult emits loading then success`() = runTest {
        val emissions = networkResult { "data" }.toList()

        assertTrue(emissions[0] is Result.Loading)
        assertEquals(Result.Success("data"), emissions[1])
    }

    @Test
    fun `networkResult maps failures to error types`() = runTest {
        val emissions = networkResult<String> { throw UnknownHostException() }.toList()

        val error = emissions.last() as Result.Error
        assertTrue(error.type is ErrorType.Network)
    }

    @Test
    fun `networkResult lets cancellation through instead of reporting it as an error`() = runTest {
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            networkResult<String> { awaitCancellation() }.toList()
        }
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
    }
}
