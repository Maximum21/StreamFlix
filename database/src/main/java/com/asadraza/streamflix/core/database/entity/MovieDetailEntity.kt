package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for detailed movie information
 *
 * Separate from MovieEntity (different data set)
 * User opens detail screen
 */
@Entity(tableName = "movie_details")
data class MovieDetailEntity(
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

    val runtime: Int?,

    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,

    @ColumnInfo(name = "vote_count")
    val voteCount: Int,

    val genres: String, // JSON string

    @ColumnInfo(name = "production_companies")
    val productionCompanies: String, // JSON string

    val tagline: String?,
    val status: String,
    val budget: Long,
    val revenue: Long,

    @ColumnInfo(name = "original_language")
    val originalLanguage: String,

    @ColumnInfo(name = "spoken_languages")
    val spokenLanguages: String, // JSON string

    val videos: String, // JSON string

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)