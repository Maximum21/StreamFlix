package com.asadraza.streamflix.core.data.util

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.Result
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

        val flow =
            networkBoundResource(
                query = { flowOf(cachedData) },
                fetch = { "Fresh Data" },
                saveFetchResult = { },
                // Cache is valid
                shouldFetch = { false }
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

        val flow =
            networkBoundResource(
                query = { flowOf(if (savedData != null) savedData else cachedData) },
                fetch = { freshData },
                saveFetchResult = { savedData = it },
                // Cache invalid
                shouldFetch = { true }
            )

        // Act & Assert
        flow.test {
            // Loading
            val loading = awaitItem()
            assertTrue(loading is Result.Loading)

            assertEquals(cachedData, (loading as Result.Loading).data)

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

        val flow =
            networkBoundResource(
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
            assertEquals(exception, (error as Result.Error).exception)

            awaitComplete()
        }
    }

    @Test
    fun `emits cached data when fetch fails but cache exists`() = runTest {
        // Arrange
        val cachedData = "Cached Data"
        val exception = RuntimeException("Network error")

        val flow =
            networkBoundResource(
                query = { flowOf(cachedData) },
                fetch = { throw exception },
                saveFetchResult = { },
                shouldFetch = { true }
            )

        // Act & Assert
        flow.test {
            // Loading
            assertTrue(awaitItem() is Result.Loading)

            // Error keeps the stale data so the UI can keep showing it
            val error = awaitItem()
            assertTrue(error is Result.Error)
            assertEquals(cachedData, (error as Result.Error).data)

            awaitComplete()
        }
    }

    @Test
    fun `an unreadable cached row is treated as a cache miss and replaced by the fetch`() = runTest {
        var saved: String? = null
        var fetchFailures = 0

        val flow =
            networkBoundResource(
                query = { flow<String?> { saved?.let { emit(it) } ?: error("bad row") } },
                fetch = { "Fresh Data" },
                saveFetchResult = { saved = it },
                shouldFetch = { it == null },
                onFetchFailed = { fetchFailures++ }
            )

        flow.test {
            val loading = awaitItem() as Result.Loading
            assertEquals(null, loading.data)

            assertEquals("Fresh Data", (awaitItem() as Result.Success).data)
            awaitComplete()
        }
        // The unreadable row is a cache miss, not a failed fetch
        assertEquals(0, fetchFailures)
    }

    @Test
    fun `a cancelled cache read is not swallowed as a cache miss`() = runTest {
        val flow = networkBoundResource<String, String>(
            query = { flow<String?> { throw CancellationException("stop") } },
            fetch = { "Fresh Data" },
            saveFetchResult = { },
            shouldFetch = { true }
        )

        try {
            flow.collect { }
            fail("cancellation must propagate")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `an Error from the cache read is not treated as a cache miss`() = runTest {
        val flow = networkBoundResource<String, String>(
            query = { flow<String?> { throw OutOfMemoryError() } },
            fetch = { "Fresh Data" },
            saveFetchResult = { },
            shouldFetch = { true }
        )

        try {
            flow.collect { }
            fail("an Error must propagate")
        } catch (_: OutOfMemoryError) {
            // expected
        }
    }
}
