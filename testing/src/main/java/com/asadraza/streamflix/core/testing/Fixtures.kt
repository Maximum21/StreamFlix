package com.asadraza.streamflix.core.testing

import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.model.movie.Video

fun testMovie(id: String = "1") = Movie(
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

fun testVideo(key: String = "abc", site: String = "YouTube", type: String = "Trailer") = Video(
    id = "v-$key",
    key = key,
    name = "Video $key",
    site = site,
    type = type,
    official = true
)

fun testMovieDetail(id: String = "1", videos: List<Video> = listOf(testVideo())) = MovieDetail(
    id = id,
    title = "Test Movie $id",
    description = "Description",
    backdropPath = null,
    posterPath = null,
    releaseDate = "2024-01-01",
    runtime = 125,
    voteAverage = 7.5,
    voteCount = 100,
    genres = listOf(Genre(28, "Action")),
    productionCompanies = emptyList(),
    tagline = null,
    status = "Released",
    budget = 0,
    revenue = 0,
    originalLanguage = "en",
    spokenLanguages = emptyList(),
    videos = videos
)
