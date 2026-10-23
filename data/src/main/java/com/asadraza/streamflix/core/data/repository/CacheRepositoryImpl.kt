package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.domain.repository.CacheRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CacheRepositoryImpl @Inject constructor(
    private val localDataSource: ILocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CacheRepository {
    override suspend fun clearCache() = withContext(ioDispatcher) { localDataSource.clearCache() }
}
