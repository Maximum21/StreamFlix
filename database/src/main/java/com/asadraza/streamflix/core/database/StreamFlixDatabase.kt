package com.asadraza.streamflix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity

/**
 * Room database
 *
 * WHY: Central database configuration
 * WHEN: App startup
 * WHERE: Singleton provided by Hilt
 */
@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StreamFlixDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
}