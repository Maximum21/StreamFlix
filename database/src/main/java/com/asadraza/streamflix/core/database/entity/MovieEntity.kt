package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for movies
 *
 * Separate from domain model (different concerns)
 * Storing/retrieving from database
 * Database layer only
 * Maps to/from domain Movie
 * DTO (network) → Domain Model (business logic) → Entity (database)
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

    @ColumnInfo(name = "genre_ids")
    val genreIds: String, // Stored as comma-separated string

    @ColumnInfo(name = "original_language")
    val originalLanguage: String,

    val adult: Boolean,

    val video: Boolean,

    /**
     * Category for filtering (trending, popular, etc.)
     * One entity can appear in multiple categories
     */
    val category: String,

    /**
     * Timestamp for cache invalidation
     * Know when to refresh from network
     */
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)