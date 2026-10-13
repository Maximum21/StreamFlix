package com.asadraza.streamflix.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * DTO for movie from API
 * Maps to domain Movie in data layer
 * Uses kotlinx.serialization
 *
 */
@Serializable
data class MovieDto(
    val id: Int,
    val title: String,
    @SerialName("overview")  // API uses "overview", we prefer "description" in domain
    val description: String,
    @SerialName("backdrop_path")  // API uses snake_case
    val backdropPath: String?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    @SerialName("original_language")
    val originalLanguage: String,
    val adult: Boolean,
    val video: Boolean
)

/**
 * API response wrapper for paginated results
 */
@Serializable
data class MovieListResponse(
    val page: Int,
    val results: List<MovieDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)