package com.asadraza.streamflix.core.model.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDetailTrailerTest {
    private fun video(key: String, type: String = "Trailer", site: String = "YouTube", official: Boolean = true) =
        Video(id = key, key = key, name = key, site = site, type = type, official = official)

    private fun detail(vararg videos: Video) = MovieDetail(
        id = "1",
        title = "T",
        description = "",
        backdropPath = null,
        posterPath = null,
        releaseDate = "",
        runtime = null,
        voteAverage = 0.0,
        voteCount = 0,
        genres = emptyList(),
        productionCompanies = emptyList(),
        tagline = null,
        status = "",
        budget = 0,
        revenue = 0,
        originalLanguage = "en",
        spokenLanguages = emptyList(),
        videos = videos.toList()
    )

    @Test
    fun `an official trailer wins over unofficial ones and teasers`() {
        val chosen = detail(
            video("teaser", type = "Teaser"),
            video("fan", official = false),
            video("official")
        ).officialTrailer

        assertEquals("official", chosen?.key)
    }

    @Test
    fun `any trailer is used when none is official`() {
        assertEquals(
            "fan",
            detail(video("teaser", type = "Teaser"), video("fan", official = false)).officialTrailer?.key
        )
    }

    @Test
    fun `a teaser is the last resort`() {
        assertEquals(
            "teaser",
            detail(video("clip", type = "Clip"), video("teaser", type = "Teaser")).officialTrailer?.key
        )
    }

    @Test
    fun `only YouTube videos qualify and clips never do`() {
        assertNull(detail(video("v", site = "Vimeo"), video("c", type = "Clip")).officialTrailer)
        assertNull(detail().officialTrailer)
    }
}
