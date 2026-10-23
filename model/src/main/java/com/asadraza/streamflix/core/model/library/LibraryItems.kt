package com.asadraza.streamflix.core.model.library

/** A title the profile saved. Title and poster are snapshotted so the list renders without a catalogue fetch. */
data class MyListItem(
    val movieId: String,
    val title: String,
    val posterPath: String?,
    val addedAt: Long
)

/** The latest trailer the profile watched for a title, with where it stopped. */
data class WatchedMovie(
    val movieId: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val watchedAt: Long,
    val positionMillis: Long
)
