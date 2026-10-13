package com.asadraza.streamflix.feature.auth
import androidx.annotation.StringRes
import com.asadraza.streamflix.core.domain.usecase.auth.ValidationError
import com.asadraza.streamflix.feature.auth.R

@StringRes
fun ValidationError.toStringRes(): Int = when (this) {
    ValidationError.EMPTY_EMAIL -> R.string.error_empty_email
    ValidationError.INVALID_EMAIL -> R.string.error_invalid_email
    ValidationError.EMPTY_PASSWORD -> R.string.error_empty_password
    ValidationError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
    ValidationError.PASSWORD_NO_UPPERCASE -> R.string.error_password_no_uppercase
    ValidationError.PASSWORD_NO_LOWERCASE -> R.string.error_password_no_lowercase
    ValidationError.PASSWORD_NO_DIGIT -> R.string.error_password_no_digit
    ValidationError.EMPTY_NAME -> R.string.error_empty_name
    ValidationError.NAME_TOO_SHORT -> R.string.error_name_too_short
    ValidationError.NAME_TOO_LONG -> R.string.error_name_too_long
}