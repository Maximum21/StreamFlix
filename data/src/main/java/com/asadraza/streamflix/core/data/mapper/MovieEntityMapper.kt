package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory

/**
 * Maps Movie (domain) to MovieEntity (database)
 * Database representation differs from domain
 * For Caching API response
 * Three separate models: DTO, Domain, Entity
 * Each layer has its own representation
 */
fun Movie.toEntity(category: MovieCategory): MovieEntity {
    return MovieEntity(
        id = id,
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genreIds.joinToString(","), // List to String for Room
        originalLanguage = originalLanguage,
        adult = adult,
        video = video,
        category = category.id,
        cachedAt = System.currentTimeMillis()
    )
}

/**
 * Maps MovieEntity (database) back to Movie (domain)
 */
fun MovieEntity.toDomain(): Movie {
    return Movie(
        id = id,
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genreIds.split(",").mapNotNull { it.toIntOrNull() }, // String to List
        originalLanguage = originalLanguage,
        adult = adult,
        video = video
    )
}

fun List<MovieEntity>.toDomain(): List<Movie> {
    return map { it.toDomain() }
}

fun List<Movie>.toEntity(category: MovieCategory): List<MovieEntity> {
    return map { it.toEntity(category) }
}