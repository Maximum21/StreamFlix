package com.asadraza.streamflix.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDto(
    val id: Int,
    val title: String,
    @SerialName("overview")
    val description: String,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    val runtime: Int?,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    val genres: List<GenreDto>,
    @SerialName("production_companies")
    val productionCompanies: List<ProductionCompanyDto>,
    val tagline: String?,
    val status: String,
    val budget: Long,
    val revenue: Long,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("spoken_languages")
    val spokenLanguages: List<SpokenLanguageDto>,
    val videos: VideosResponse
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String
)

@Serializable
data class ProductionCompanyDto(
    val id: Int,
    val name: String,
    @SerialName("logo_path")
    val logoPath: String?,
    @SerialName("origin_country")
    val originCountry: String
)

@Serializable
data class SpokenLanguageDto(
    @SerialName("iso_639_1")
    val code: String,
    val name: String
)

@Serializable
data class VideosResponse(
    val results: List<VideoDto>
)

@Serializable
data class VideoDto(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean
)