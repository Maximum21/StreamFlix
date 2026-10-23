package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Membership of a movie in a category list, with its position in that list.
 * Deleting a movie removes its memberships.
 */
@Entity(
    tableName = "movie_category",
    primaryKeys = ["category", "movie_id"],
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movie_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category", "position"]),
        Index(value = ["movie_id"])
    ]
)
data class MovieCategoryEntity(
    val category: String,
    @ColumnInfo(name = "movie_id")
    val movieId: String,
    val position: Int
)
