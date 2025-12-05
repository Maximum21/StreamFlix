package com.asadraza.streamflix.core.database.di

import android.content.Context
import androidx.room.Room
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.database.StreamFlixDatabase
import com.asadraza.streamflix.core.database.dao.MovieDao
import com.asadraza.streamflix.core.database.dao.MovieDetailDao
import com.asadraza.streamflix.core.database.dao.RemoteKeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): StreamFlixDatabase {
        return Room.databaseBuilder(
            context,
            StreamFlixDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: StreamFlixDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideMovieDetailDao(database: StreamFlixDatabase): MovieDetailDao {
        return database.movieDetailDao()
    }

    @Provides
    @Singleton
    fun provideRemoteKeyDao(database: StreamFlixDatabase): RemoteKeyDao {
        return database.remoteKeyDao()
    }
}