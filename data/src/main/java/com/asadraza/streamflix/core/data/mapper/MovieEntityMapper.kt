package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.network.model.MovieDto

/**
 * The single place a TMDB movie becomes a cache row. Both category and search paging use it.
 */
fun MovieDto.toEntity(cachedAt: Long = System.currentTimeMillis()): MovieEntity = MovieEntity(
    id = id.toString(),
    title = title,
    description = description,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds.joinToString(","),
    originalLanguage = originalLanguage,
    adult = adult,
    video = video,
    cachedAt = cachedAt
)

fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    description = description,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds.split(",").mapNotNull { it.toIntOrNull() },
    originalLanguage = originalLanguage,
    adult = adult,
    video = video
)
