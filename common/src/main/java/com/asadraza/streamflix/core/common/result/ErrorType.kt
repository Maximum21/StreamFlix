package com.asadraza.streamflix.core.common.result

import com.asadraza.streamflix.core.common.error.DatabaseError
import com.asadraza.streamflix.core.common.error.NetworkError

/**
 * Categorised failures. Types carry codes and reasons, never display text: the UI layer maps
 * them to localised strings.
 */
sealed class ErrorType {
    data class Network(val error: NetworkError) : ErrorType()

    data class Database(val error: DatabaseError) : ErrorType()

    data class Validation(val reason: ValidationReason) : ErrorType()

    data class Authorization(val reason: AuthReason) : ErrorType()

    data class Http(val code: Int) : ErrorType() {
        fun isClientError(): Boolean = code in 400..499

        fun isServerError(): Boolean = code in 500..599
    }

    data class Unknown(val throwable: Throwable?) : ErrorType()
}

enum class ValidationReason {
    NameBlank,
    NameTooLong,
    MaxProfilesReached,
    LastProfile
}

enum class AuthReason {
    NotSignedIn,
    SessionExpired,
    EmailNotVerified,
    InvalidCredentials,
    EmailAlreadyInUse,
    WeakPassword,
    TooManyRequests,
    Forbidden
}

fun ErrorType.isRetryable(): Boolean = when (this) {
    is ErrorType.Network -> true
    is ErrorType.Http -> isServerError()
    is ErrorType.Database,
    is ErrorType.Validation,
    is ErrorType.Authorization,
    is ErrorType.Unknown -> false
}

fun ErrorType.requiresAuth(): Boolean = when (this) {
    is ErrorType.Authorization -> reason == AuthReason.SessionExpired || reason == AuthReason.NotSignedIn
    is ErrorType.Http -> code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN
    else -> false
}

fun ErrorType.isNetworkError(): Boolean = this is ErrorType.Network

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
