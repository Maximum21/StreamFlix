package com.asadraza.streamflix.core.ui.text

import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.ui.R

fun ErrorType.toUiText(): UiText = when (this) {
    is ErrorType.Network -> UiText.Resource(error.stringRes())
    is ErrorType.Database -> UiText.Resource(R.string.error_database)
    is ErrorType.Validation -> UiText.Resource(reason.stringRes())
    is ErrorType.Authorization -> UiText.Resource(reason.stringRes())
    is ErrorType.Http -> httpText()
    is ErrorType.Unknown -> UiText.Resource(R.string.error_unknown)
}

private fun NetworkError.stringRes(): Int = when (this) {
    NetworkError.NoInternet -> R.string.error_no_internet
    NetworkError.Timeout -> R.string.error_timeout
    NetworkError.RateLimitExceeded -> R.string.error_rate_limit
    NetworkError.ParsingError -> R.string.error_parsing
    is NetworkError.ServerError -> R.string.error_server
    is NetworkError.Unknown -> R.string.error_network_generic
}

private fun ValidationReason.stringRes(): Int = when (this) {
    ValidationReason.NameBlank -> R.string.validation_name_blank
    ValidationReason.NameTooLong -> R.string.validation_name_too_long
    ValidationReason.MaxProfilesReached -> R.string.validation_max_profiles
    ValidationReason.LastProfile -> R.string.validation_last_profile
}

private fun AuthReason.stringRes(): Int = when (this) {
    AuthReason.NotSignedIn -> R.string.auth_not_signed_in
    AuthReason.SessionExpired -> R.string.auth_session_expired
    AuthReason.EmailNotVerified -> R.string.auth_email_not_verified
    AuthReason.InvalidCredentials -> R.string.auth_invalid_credentials
    AuthReason.EmailAlreadyInUse -> R.string.auth_email_in_use
    AuthReason.WeakPassword -> R.string.auth_weak_password
    AuthReason.TooManyRequests -> R.string.auth_too_many_requests
    AuthReason.Forbidden -> R.string.auth_forbidden
}

private fun ErrorType.Http.httpText(): UiText = when {
    code == HTTP_NOT_FOUND -> UiText.Resource(R.string.error_http_not_found)
    isClientError() -> UiText.Resource(R.string.error_http_client, code)
    isServerError() -> UiText.Resource(R.string.error_http_server)
    else -> UiText.Resource(R.string.error_http_generic, code)
}

private const val HTTP_NOT_FOUND = 404
