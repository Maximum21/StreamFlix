package com.asadraza.streamflix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.dao.RemoteKeyDao
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity

/**
 * Room database
 *
 * WHY: Central database configuration
 * WHEN: App startup
 * WHERE: Singleton provided by Hilt
 *
 * VERSION HISTORY:
 * - v1: Initial schema (movies, movie_details)
 * - v2: Added remote_keys for Paging 3 RemoteMediator
 */
@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        RemoteKeyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StreamFlixDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}