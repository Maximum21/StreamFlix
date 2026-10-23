package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorMapper
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.data.source.LastProfileException
import com.asadraza.streamflix.core.data.source.ProfileLimitReachedException
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Explicit mapping of Firebase failures. User-not-found and wrong-password both map to
 * InvalidCredentials so the app never reveals whether an email is registered.
 */
fun Throwable.toDataErrorType(): ErrorType = when (this) {
    is UnauthenticatedException -> ErrorType.Authorization(AuthReason.NotSignedIn)
    is ProfileLimitReachedException -> ErrorType.Validation(ValidationReason.MaxProfilesReached)
    is LastProfileException -> ErrorType.Validation(ValidationReason.LastProfile)
    is FirebaseNetworkException -> ErrorType.Network(NetworkError.NoInternet)
    is FirebaseTooManyRequestsException -> ErrorType.Authorization(AuthReason.TooManyRequests)
    // WeakPassword extends InvalidCredentials, so it has to be matched first
    is FirebaseAuthWeakPasswordException -> ErrorType.Authorization(AuthReason.WeakPassword)
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> ErrorType.Authorization(AuthReason.InvalidCredentials)
    is FirebaseAuthUserCollisionException -> ErrorType.Authorization(AuthReason.EmailAlreadyInUse)
    is FirebaseAuthEmailException -> ErrorType.Unknown(this)
    is FirebaseAuthException -> ErrorType.Authorization(AuthReason.InvalidCredentials)
    is FirebaseFirestoreException -> toFirestoreErrorType()
    else -> ErrorMapper.map(this)
}

private fun FirebaseFirestoreException.toFirestoreErrorType(): ErrorType = when (code) {
    FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorType.Network(NetworkError.NoInternet)
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ErrorType.Network(NetworkError.Timeout)
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> ErrorType.Network(NetworkError.RateLimitExceeded)
    FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorType.Authorization(AuthReason.Forbidden)
    FirebaseFirestoreException.Code.UNAUTHENTICATED -> ErrorType.Authorization(AuthReason.SessionExpired)
    else -> ErrorType.Unknown(this)
}
