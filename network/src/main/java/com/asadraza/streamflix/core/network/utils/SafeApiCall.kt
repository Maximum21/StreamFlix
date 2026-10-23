package com.asadraza.streamflix.core.network.utils

import com.asadraza.streamflix.core.common.error.NetworkError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

suspend fun <T> safeApiCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (e: CancellationException) {
        // Cancellation must reach the caller's scope, never become an error value
        throw e
    } catch (e: NetworkError) {
        throw e
    } catch (e: Exception) {
        throw e.toNetworkError()
    }
}

// Order matters: timeouts and unknown hosts are IOExceptions, so they are matched first
private fun Exception.toNetworkError(): NetworkError = when (this) {
    is HttpException -> httpError(code())
    is UnknownHostException -> NetworkError.NoInternet
    is SocketTimeoutException -> NetworkError.Timeout
    is SerializationException -> NetworkError.ParsingError
    is IOException -> NetworkError.NoInternet
    else -> NetworkError.Unknown(message ?: "Unknown error")
}

private fun httpError(code: Int): NetworkError = when (code) {
    401, 403 -> NetworkError.ServerError(code, "Authentication failed")
    404 -> NetworkError.ServerError(code, "Resource not found")
    429 -> NetworkError.RateLimitExceeded
    in 500..599 -> NetworkError.ServerError(code, "Server Error")
    else -> NetworkError.ServerError(code, "HTTP $code")
}
