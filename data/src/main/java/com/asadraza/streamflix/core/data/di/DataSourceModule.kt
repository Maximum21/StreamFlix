package com.asadraza.streamflix.core.data.di

import com.asadraza.streamflix.core.data.source.AuthDataSource
import com.asadraza.streamflix.core.data.source.CurrentProfileStore
import com.asadraza.streamflix.core.data.source.DataStoreCurrentProfileStore
import com.asadraza.streamflix.core.data.source.MyListDataSource
import com.asadraza.streamflix.core.data.source.ProfileDataSource
import com.asadraza.streamflix.core.data.source.WatchHistoryDataSource
import com.asadraza.streamflix.core.data.source.firebase.FirebaseAuthDataSource
import com.asadraza.streamflix.core.data.source.firebase.FirestoreMyListDataSource
import com.asadraza.streamflix.core.data.source.firebase.FirestoreProfileDataSource
import com.asadraza.streamflix.core.data.source.firebase.FirestoreWatchHistoryDataSource
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.database.datasource.LocalDataSource
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.asadraza.streamflix.core.network.datasource.RemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings for the sources the repositories read from and write to. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    abstract fun bindRemoteDataSource(remoteDataSource: RemoteDataSource): IRemoteDataSource

    @Binds
    abstract fun bindLocalDataSource(localDataSource: LocalDataSource): ILocalDataSource

    @Binds
    abstract fun bindAuthDataSource(impl: FirebaseAuthDataSource): AuthDataSource

    @Binds
    abstract fun bindProfileDataSource(impl: FirestoreProfileDataSource): ProfileDataSource

    @Binds
    abstract fun bindCurrentProfileStore(impl: DataStoreCurrentProfileStore): CurrentProfileStore

    @Binds
    abstract fun bindMyListDataSource(impl: FirestoreMyListDataSource): MyListDataSource

    @Binds
    abstract fun bindWatchHistoryDataSource(impl: FirestoreWatchHistoryDataSource): WatchHistoryDataSource
}
