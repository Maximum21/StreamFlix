package com.asadraza.streamflix.core.common.result

import com.asadraza.streamflix.core.common.error.NetworkError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorTypeTest {
    @Test
    fun `only transient failures are retryable`() {
        assertTrue(ErrorType.Network(NetworkError.NoInternet).isRetryable())
        assertTrue(ErrorType.Http(503).isRetryable())
        assertFalse(ErrorType.Http(404).isRetryable())
        assertFalse(ErrorType.Validation(ValidationReason.NameBlank).isRetryable())
        assertFalse(ErrorType.Authorization(AuthReason.InvalidCredentials).isRetryable())
        assertFalse(ErrorType.Unknown(null).isRetryable())
    }

    @Test
    fun `requiresAuth covers expired sessions and 401 or 403`() {
        assertTrue(ErrorType.Authorization(AuthReason.SessionExpired).requiresAuth())
        assertTrue(ErrorType.Authorization(AuthReason.NotSignedIn).requiresAuth())
        assertFalse(ErrorType.Authorization(AuthReason.InvalidCredentials).requiresAuth())
        assertTrue(ErrorType.Http(401).requiresAuth())
        assertTrue(ErrorType.Http(403).requiresAuth())
        assertFalse(ErrorType.Http(500).requiresAuth())
        assertFalse(ErrorType.Unknown(null).requiresAuth())
    }

    @Test
    fun `http helpers classify the status range`() {
        assertTrue(ErrorType.Http(404).isClientError())
        assertTrue(ErrorType.Http(500).isServerError())
        assertFalse(ErrorType.Http(500).isClientError())
        assertTrue(ErrorType.Network(NetworkError.Timeout).isNetworkError())
    }

    @Test
    fun `mapper converts platform exceptions`() {
        assertEquals(ErrorType.Network(NetworkError.NoInternet), UnknownHostException().toErrorType())
        assertEquals(ErrorType.Network(NetworkError.Timeout), SocketTimeoutException().toErrorType())
        assertEquals(ErrorType.Network(NetworkError.Unknown("io")), IOException("io").toErrorType())
        assertEquals(ErrorType.Network(NetworkError.Unknown("Network error")), IOException().toErrorType())
        assertEquals(ErrorType.Network(NetworkError.ParsingError), NetworkError.ParsingError.toErrorType())
        val other = IllegalStateException()
        assertEquals(ErrorType.Unknown(other), other.toErrorType())
    }

    @Test
    fun `toResultError keeps the failure, type and stale data`() {
        val failure = SocketTimeoutException()

        val result = failure.toResultError(data = 7)

        assertEquals(failure, result.exception)
        assertEquals(ErrorType.Network(NetworkError.Timeout), result.type)
        assertEquals(7, result.data)
    }
}
