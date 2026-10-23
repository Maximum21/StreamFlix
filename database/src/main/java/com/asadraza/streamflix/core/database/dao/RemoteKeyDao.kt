package com.asadraza.streamflix.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity

/**
 * Pagination state for RemoteMediator, one row per category or search query.
 */
@Dao
interface RemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remoteKey: RemoteKeyEntity)

    @Query("SELECT * FROM remote_keys WHERE id = :id")
    suspend fun getRemoteKeyById(id: String): RemoteKeyEntity?

    @Query("DELETE FROM remote_keys WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM remote_keys")
    suspend fun deleteAll()

    @Query("SELECT created_at FROM remote_keys WHERE id = :id")
    suspend fun getCreationTime(id: String): Long?
}
