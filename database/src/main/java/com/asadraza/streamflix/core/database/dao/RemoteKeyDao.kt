package com.asadraza.streamflix.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity

/**
 * DAO for remote keys (pagination state)
 *
 * Used by RemoteMediator to track pagination
 * Each category/query has its own set of keys
 */
@Dao
interface RemoteKeyDao {

    /**
     * Insert remote keys for a batch of movies
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remoteKey: RemoteKeyEntity)

    /**
     * Get remote key for a specific movie in a category/query
     */
    @Query("SELECT * FROM remote_keys WHERE id = :id")
    suspend fun getRemoteKeyById(id: String): RemoteKeyEntity?

    /**
     * Get remote keys for a category/query
     * Used to find the last page loaded
     */
    @Query("SELECT * FROM remote_keys WHERE category_or_query = :categoryOrQuery ORDER BY current_page DESC LIMIT 1")
    suspend fun getLatestRemoteKey(categoryOrQuery: String): RemoteKeyEntity?

    /**
     * Clear remote keys for a category/query
     * Called on refresh to reset pagination
     */
    @Query("DELETE FROM remote_keys WHERE category_or_query = :categoryOrQuery")
    suspend fun clearRemoteKeys(categoryOrQuery: String)

    /**
     * Clear all remote keys
     */
    @Query("DELETE FROM remote_keys")
    suspend fun clearAllRemoteKeys()

    /**
     * Get creation time for cache invalidation
     */
    @Query("SELECT created_at FROM remote_keys WHERE category_or_query = :categoryOrQuery LIMIT 1")
    suspend fun getCreationTime(categoryOrQuery: String): Long?
}