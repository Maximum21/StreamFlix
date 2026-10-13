package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.model.movie.MovieDetail
import com.asadraza.streamflix.core.model.movie.ProductionCompany
import com.asadraza.streamflix.core.model.movie.SpokenLanguage
import com.asadraza.streamflix.core.model.movie.Video
import com.asadraza.streamflix.core.network.model.GenreDto
import com.asadraza.streamflix.core.network.model.MovieDetailDto
import com.asadraza.streamflix.core.network.model.ProductionCompanyDto
import com.asadraza.streamflix.core.network.model.SpokenLanguageDto
import com.asadraza.streamflix.core.network.model.VideoDto

fun MovieDetailDto.toDomain(): MovieDetail {
    return MovieDetail(
        id = id.toString(),
        title = title,
        description = description,
        backdropPath = backdropPath,
        posterPath = posterPath,
        releaseDate = releaseDate,
        runtime = runtime,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genres = genres.map { it.toDomain() },
        productionCompanies = productionCompanies.map { it.toDomain() },
        tagline = tagline,
        status = status,
        budget = budget,
        revenue = revenue,
        originalLanguage = originalLanguage,
        spokenLanguages = spokenLanguages.map { it.toDomain() },
        videos = videos.results.map { it.toDomain() }
    )
}

fun GenreDto.toDomain(): Genre {
    return Genre(id = id, name = name)
}

fun ProductionCompanyDto.toDomain(): ProductionCompany {
    return ProductionCompany(
        id = id,
        name = name,
        logoPath = logoPath,
        originCountry = originCountry
    )
}

fun SpokenLanguageDto.toDomain(): SpokenLanguage {
    return SpokenLanguage(code = code, name = name)
}

fun VideoDto.toDomain(): Video {
    return Video(
        id = id,
        key = key,
        name = name,
        site = site,
        type = type,
        official = official
    )
}