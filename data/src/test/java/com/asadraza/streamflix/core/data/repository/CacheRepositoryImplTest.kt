package com.asadraza.streamflix.core.data.repository

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheRepositoryImplTest {
    @Test
    fun `clearing the cache clears the local data source`() = runTest {
        val local = mockk<ILocalDataSource>(relaxed = true)

        val result = CacheRepositoryImpl(local, UnconfinedTestDispatcher(testScheduler)).clearCache()

        coVerify(exactly = 1) { local.clearCache() }
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `a storage failure is reported instead of crashing the caller`() = runTest {
        val local = mockk<ILocalDataSource>()
        coEvery { local.clearCache() } throws IllegalStateException("disk full")

        val result = CacheRepositoryImpl(local, UnconfinedTestDispatcher(testScheduler)).clearCache()

        assertTrue(result is Result.Error)
    }
}
