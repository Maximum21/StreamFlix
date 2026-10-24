package com.asadraza.streamflix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pagination state for one list: a category or a search query.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    // e.g. "category_trending" or "search_batman"
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "prev_key")
    val prevKey: Int?,
    @ColumnInfo(name = "next_key")
    val nextKey: Int?,
    @ColumnInfo(name = "current_page")
    val currentPage: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

object RemoteKeyIds {
    fun category(categoryId: String) = "category_$categoryId"

    fun search(query: String) = "search_$query"

    /** For lists stored under an explicit list id, such as a genre selection. */
    fun list(listId: String) = "list_$listId"
}
