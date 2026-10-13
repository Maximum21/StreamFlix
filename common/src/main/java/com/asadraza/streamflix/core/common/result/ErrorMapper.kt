package com.asadraza.streamflix.core.common.result

import com.asadraza.streamflix.core.common.error.DatabaseError
import com.asadraza.streamflix.core.common.error.NetworkError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps exceptions to ErrorType
 */
object ErrorMapper {

    /**
     * Convert any Throwable to ErrorType
     */
    fun map(throwable: Throwable): ErrorType = when (throwable) {
        // Network errors (custom)
        is NetworkError -> ErrorType.Network(throwable)

        // Database errors (custom)
        is DatabaseError -> ErrorType.Database(throwable)

        // HTTP errors (Retrofit)
        is HttpException -> mapHttpException(throwable)

        // Network connectivity errors
        is UnknownHostException -> ErrorType.Network(NetworkError.NoInternet)
        is SocketTimeoutException -> ErrorType.Network(NetworkError.Timeout)
        is IOException -> ErrorType.Network(NetworkError.Unknown(throwable.message ?: "Network error"))

        // Unknown errors
        else -> ErrorType.Unknown(throwable)
    }

    /**
     * Map HTTP status codes to specific error types
     */
    private fun mapHttpException(exception: HttpException): ErrorType {
        val code = exception.code()
        val message = exception.message()

        return when (code) {
            // Authorization errors
            401 -> ErrorType.Authorization("Unauthorized. Please log in")
            403 -> ErrorType.Authorization("Access forbidden")

            // Client errors
            404 -> ErrorType.Http(404, "Content not found")
            429 -> ErrorType.Network(NetworkError.RateLimitExceeded)
            in 400..499 -> ErrorType.Http(code, message)

            // Server errors
            500 -> ErrorType.Http(500, "Internal server error")
            502 -> ErrorType.Http(502, "Bad gateway")
            503 -> ErrorType.Http(503, "Service unavailable")
            in 500..599 -> ErrorType.Http(code, message)

            // Other
            else -> ErrorType.Http(code, message)
        }
    }
}

/**
 * Extension function for easy error mapping
 */
fun Throwable.toErrorType(): ErrorType = ErrorMapper.map(this)

/**
 * Create Result.Error from exception
 */
fun <T> Throwable.toResultError(data: T? = null): Result.Error<T> {
    return Result.Error(
        exception = this,
        type = this.toErrorType(),
        data = data
    )
}