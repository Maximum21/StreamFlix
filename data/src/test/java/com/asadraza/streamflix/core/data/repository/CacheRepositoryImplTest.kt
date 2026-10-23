package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheRepositoryImplTest {
    @Test
    fun `clearing the cache clears the local data source`() = runTest {
        val local = mockk<ILocalDataSource>(relaxed = true)

        CacheRepositoryImpl(local, UnconfinedTestDispatcher(testScheduler)).clearCache()

        coVerify(exactly = 1) { local.clearCache() }
    }
}
