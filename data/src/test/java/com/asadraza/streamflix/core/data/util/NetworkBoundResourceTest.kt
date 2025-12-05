package com.asadraza.streamflix.core.data.util

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for NetworkBoundResource
 *
 * WHY: Critical caching logic needs comprehensive tests
 * WHEN: After implementing NetworkBoundResource
 * WHERE: Data layer tests
 * HOW: Use Turbine for Flow testing
 */
class NetworkBoundResourceTest {

    @Test
    fun `emits Loading then Success when cache is valid`() = runTest {
        // Arrange
        val cachedData = "Cached Data"

        val flow = networkBoundResource(
            query = { flowOf(cachedData) },
            fetch = { "Fresh Data" },
            saveFetchResult = { },
            shouldFetch = { false } // Cache is valid
        )

        // Act & Assert
        flow.test {
            // Should emit Loading
            val loading = awaitItem()
            assertTrue(loading is Result.Loading)

            // Should emit cached Success
            val success = awaitItem()
            assertTrue(success is Result.Success)
            assertEquals(cachedData, (success as Result.Success).data)

            awaitComplete()
        }
    }

    @Test
    fun `fetches and saves when cache is invalid`() = runTest {
        // Arrange
        val cachedData = "Old Data"
        val freshData = "Fresh Data"
        var savedData: String? = null

        val flow = networkBoundResource(
            query = { flowOf(if (savedData != null) savedData else cachedData) },
            fetch = { freshData },
            saveFetchResult = { savedData = it },
            shouldFetch = { true } // Cache invalid
        )

        // Act & Assert
        flow.test {
            // Loading
            val loading = awaitItem()
            assertTrue(loading is Result.Loading)

            // Old cached data while fetching
            val cached = awaitItem()
            assertTrue(cached is Result.Success)
            assertEquals(cachedData, (cached as Result.Success).data)

            // Fresh data after fetch
            val success = awaitItem()
            assertTrue(success is Result.Success)
            assertEquals(freshData, (success as Result.Success).data)

            // Verify data was saved
            assertEquals(freshData, savedData)

            awaitComplete()
        }
    }

    @Test
    fun `emits Error when fetch fails and no cache`() = runTest {
        // Arrange
        val exception = RuntimeException("Network error")

        val flow = networkBoundResource(
            query = { flowOf(null) },
            fetch = { throw exception },
            saveFetchResult = { },
            shouldFetch = { true }
        )

        // Act & Assert
        flow.test {
            // Loading
            assertTrue(awaitItem() is Result.Loading)

            // Error
            val error = awaitItem()
            assertTrue(error is Result.Error)
            assertEquals(exception, (error as Result.Error))

            awaitComplete()
        }
    }

    @Test
    fun `emits cached data when fetch fails but cache exists`() = runTest {
        // Arrange
        val cachedData = "Cached Data"
        val exception = RuntimeException("Network error")

        val flow = networkBoundResource(
            query = { flowOf(cachedData) },
            fetch = { throw exception },
            saveFetchResult = { },
            shouldFetch = { true }
        )

        // Act & Assert
        flow.test {
            // Loading
            assertTrue(awaitItem() is Result.Loading)

            // Cached data shown during fetch
            val cached1 = awaitItem()
            assertTrue(cached1 is Result.Success)

            // Still showing cached data after error
            val cached2 = awaitItem()
            assertTrue(cached2 is Result.Success)
            assertEquals(cachedData, (cached2 as Result.Success).data)

            awaitComplete()
        }
    }
}