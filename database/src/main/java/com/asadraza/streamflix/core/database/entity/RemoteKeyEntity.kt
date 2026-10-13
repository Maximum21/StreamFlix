package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Remote keys for Paging 3 RemoteMediator
 *
 * Stores pagination state for each category/query
 * Used to determine which page to load next
 *
 * WHY SEPARATE TABLE?
 * - Movies can appear in multiple categories
 * - Each category has its own pagination state
 * - Clean separation of concerns
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey
    val id: String, // Format: "category_movieId" or "search_query_movieId"

    @ColumnInfo(name = "category_or_query")
    val categoryOrQuery: String, // e.g., "trending", "popular", "search:batman"

    @ColumnInfo(name = "prev_key")
    val prevKey: Int?,

    @ColumnInfo(name = "next_key")
    val nextKey: Int?,

    @ColumnInfo(name = "current_page")
    val currentPage: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)