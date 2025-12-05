package com.asadraza.streamflix.core.data.di

import com.asadraza.streamflix.core.data.repository.AuthRepositoryImpl
import com.asadraza.streamflix.core.data.repository.MovieRepositoryImpl
import com.asadraza.streamflix.core.data.repository.ProfileRepositoryImpl
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.datasource.LocalDataSource
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.domain.repository.MovieDiscoveryRepository
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.datasource.RemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection module for repositories and data sources
 *
 * @Binds:
 * - More efficient than @Provides for simple interface-to-impl binding
 * - Tells Hilt When someone asks for MovieCatalogRepository, give MovieRepositoryImpl
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    //  Data Source Bindings
    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        remoteDataSource: RemoteDataSource
    ): IRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindLocalDataSource(
        localDataSource: LocalDataSource
    ): ILocalDataSource

    //  Movie Repository Bindings
    @Binds
    @Singleton
    abstract fun bindMovieCatalogRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieCatalogRepository

    @Binds
    @Singleton
    abstract fun bindMovieDetailRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieDetailRepository

    @Binds
    @Singleton
    abstract fun bindMovieSearchRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieSearchRepository

    @Binds
    @Singleton
    abstract fun bindMovieDiscoveryRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieDiscoveryRepository

    // Auth Repository Binding
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    // Profile Repository Binding
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository
}