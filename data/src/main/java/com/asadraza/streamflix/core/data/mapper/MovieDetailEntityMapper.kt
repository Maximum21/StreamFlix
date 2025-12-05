package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.model.movie.ProductionCompany
import com.asadraza.streamflix.core.model.movie.SpokenLanguage
import com.asadraza.streamflix.core.model.movie.Video
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maps MovieDetail to Entity with JSON serialization
 *
 * Complex nested objects stored as JSON in database
 * Caching detailed movie info
 * Data layer
 *
 * NOTE: Using kotlinx.serialization for JSON
 */
fun MovieDetail.toEntity(): MovieDetailEntity {
    return MovieDetailEntity(
        id = id,
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        runtime = runtime,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = Json.encodeToString(genres),
        productionCompanies = Json.encodeToString(productionCompanies),
        tagline = tagline,
        status = status,
        budget = budget,
        revenue = revenue,
        originalLanguage = originalLanguage,
        spokenLanguages = Json.encodeToString(spokenLanguages),
        videos = Json.encodeToString(videos),
        cachedAt = System.currentTimeMillis()
    )
}

fun MovieDetailEntity.toDomain(): MovieDetail {
    return MovieDetail(
        id = id,
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        runtime = runtime,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = Json.decodeFromString<List<Genre>>(genres),
        productionCompanies = Json.decodeFromString<List<ProductionCompany>>(productionCompanies),
        tagline = tagline,
        status = status,
        budget = budget,
        revenue = revenue,
        originalLanguage = originalLanguage,
        spokenLanguages = Json.decodeFromString<List<SpokenLanguage>>(spokenLanguages),
        videos = Json.decodeFromString<List<Video>>(videos)
    )
}