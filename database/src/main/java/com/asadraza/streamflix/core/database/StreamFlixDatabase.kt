package com.asadraza.streamflix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.dao.RemoteKeyDao
import com.asadraza.streamflix.core.database.entity.MovieCategoryEntity
import com.asadraza.streamflix.core.database.entity.MovieDetailEntity
import com.asadraza.streamflix.core.database.entity.MovieEntity
import com.asadraza.streamflix.core.database.entity.RemoteKeyEntity
import com.asadraza.streamflix.core.database.entity.SearchResultEntity

@Database(
    entities = [
        MovieEntity::class,
        MovieCategoryEntity::class,
        SearchResultEntity::class,
        MovieDetailEntity::class,
        RemoteKeyEntity::class
    ],
    version = DatabaseConfig.VERSION,
    exportSchema = true
)
abstract class StreamFlixDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    abstract fun movieDetailDao(): MovieDetailDao

    abstract fun remoteKeyDao(): RemoteKeyDao
}
