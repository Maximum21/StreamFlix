package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.network.model.MovieDto

/**
 * Maps MovieDto (network) to Movie (domain)
 *
 * Separate concerns - network layer doesn't know about domain
 * After receiving API response
 * Extension function for clean syntax
 */
fun MovieDto.toDomain(): Movie {
    return Movie(
        id = id.toString(),
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genreIds,
        originalLanguage = originalLanguage,
        adult = adult,
        video = video
    )
}

/**
 * Maps list of DTOs to domain models
 * To Avoid mapping logic in repository
 */
fun List<MovieDto>.toDomain(): List<Movie> {
    return map { it.toDomain() }
}