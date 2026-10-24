package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One canonical row per movie. List membership lives in [MovieCategoryEntity] and
 * [SearchResultEntity], so the same movie can appear in several lists without duplication.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String?,
    @ColumnInfo(name = "poster_path")
    val posterPath: String?,
    @ColumnInfo(name = "release_date")
    val releaseDate: String,
    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,
    @ColumnInfo(name = "vote_count")
    val voteCount: Int,
    // Comma-separated TMDB genre ids
    @ColumnInfo(name = "genre_ids")
    val genreIds: String,
    @ColumnInfo(name = "original_language")
    val originalLanguage: String,
    val adult: Boolean,
    val video: Boolean,
    @ColumnInfo(name = "cached_at", index = true)
    val cachedAt: Long = System.currentTimeMillis()
)
