package com.asadraza.streamflix.core.common.result

import com.asadraza.streamflix.core.common.error.DatabaseError
import com.asadraza.streamflix.core.common.error.NetworkError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps a throwable to an [ErrorType]. HTTP failures arrive here already converted to
 * [NetworkError] by the network module, and Firebase failures are mapped in the data module,
 * so this stays free of both SDKs.
 */
object ErrorMapper {
    fun map(throwable: Throwable): ErrorType = when (throwable) {
        is NetworkError -> ErrorType.Network(throwable)
        is DatabaseError -> ErrorType.Database(throwable)
        is UnknownHostException -> ErrorType.Network(NetworkError.NoInternet)
        is SocketTimeoutException -> ErrorType.Network(NetworkError.Timeout)
        is IOException -> ErrorType.Network(NetworkError.Unknown(throwable.message ?: "Network error"))
        else -> ErrorType.Unknown(throwable)
    }
}

fun Throwable.toErrorType(): ErrorType = ErrorMapper.map(this)

fun <T> Throwable.toResultError(data: T? = null): Result.Error<T> = Result.Error(
    exception = this,
    type = toErrorType(),
    data = data
)
