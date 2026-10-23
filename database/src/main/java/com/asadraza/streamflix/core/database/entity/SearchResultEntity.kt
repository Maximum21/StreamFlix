package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cached result of one search query, so different queries never share rows.
 */
@Entity(
    tableName = "search_result",
    primaryKeys = ["query", "movie_id"],
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movie_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["query", "position"]),
        Index(value = ["movie_id"]),
        Index(value = ["created_at"])
    ]
)
data class SearchResultEntity(
    val query: String,
    @ColumnInfo(name = "movie_id")
    val movieId: String,
    val position: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
