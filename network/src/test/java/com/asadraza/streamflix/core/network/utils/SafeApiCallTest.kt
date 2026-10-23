package com.asadraza.streamflix.core.network.utils

import com.asadraza.streamflix.core.common.error.NetworkError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SafeApiCallTest {
    private suspend fun failure(block: suspend () -> Unit): Throwable {
        try {
            safeApiCall { block() }
        } catch (t: Throwable) {
            return t
        }
        fail("Expected a failure")
        error("unreachable")
    }

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `returns the value on success`() = runTest {
        assertEquals(42, safeApiCall { 42 })
    }

    @Test
    fun `maps http statuses`() = runTest {
        assertEquals(NetworkError.RateLimitExceeded, failure { throw http(429) })
        assertEquals(NetworkError.ServerError(404, "Resource not found"), failure { throw http(404) })
        assertEquals(NetworkError.ServerError(401, "Authentication failed"), failure { throw http(401) })
        assertEquals(NetworkError.ServerError(503, "Server Error"), failure { throw http(503) })
        assertEquals(NetworkError.ServerError(418, "HTTP 418"), failure { throw http(418) })
    }

    @Test
    fun `maps connectivity failures`() = runTest {
        assertEquals(NetworkError.NoInternet, failure { throw UnknownHostException() })
        assertEquals(NetworkError.Timeout, failure { throw SocketTimeoutException() })
        assertEquals(NetworkError.NoInternet, failure { throw IOException("reset") })
    }

    @Test
    fun `maps malformed payloads`() = runTest {
        assertEquals(NetworkError.ParsingError, failure { throw SerializationException("bad json") })
    }

    @Test
    fun `wraps unexpected errors`() = runTest {
        val error = failure { throw IllegalStateException("boom") }
        assertTrue(error is NetworkError.Unknown)
        assertEquals("boom", error.message)
    }

    @Test
    fun `does not swallow cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, failure { throw cancellation })
    }
}
