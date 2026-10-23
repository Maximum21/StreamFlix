package com.asadraza.streamflix.core.data.mapper

import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.data.source.LastProfileException
import com.asadraza.streamflix.core.data.source.ProfileLimitReachedException
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseErrorMapperTest {
    private fun map(t: Throwable) = t.toDataErrorType()

    @Test
    fun `wrong password and unknown user are indistinguishable`() {
        val invalid = ErrorType.Authorization(AuthReason.InvalidCredentials)

        assertEquals(invalid, map(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad")))
        assertEquals(invalid, map(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")))
    }

    @Test
    fun `account errors map to reasons`() {
        assertEquals(
            ErrorType.Authorization(AuthReason.EmailAlreadyInUse),
            map(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken"))
        )
        assertEquals(
            ErrorType.Authorization(AuthReason.WeakPassword),
            map(FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short"))
        )
        assertEquals(
            ErrorType.Authorization(AuthReason.TooManyRequests),
            map(FirebaseTooManyRequestsException("slow down"))
        )
    }

    @Test
    fun `connectivity failures map to network errors`() {
        assertEquals(ErrorType.Network(NetworkError.NoInternet), map(FirebaseNetworkException("offline")))
        assertEquals(
            ErrorType.Network(NetworkError.NoInternet),
            map(FirebaseFirestoreException("down", Code.UNAVAILABLE))
        )
        assertEquals(
            ErrorType.Network(NetworkError.Timeout),
            map(FirebaseFirestoreException("slow", Code.DEADLINE_EXCEEDED))
        )
    }

    @Test
    fun `firestore permission and session failures map to authorization`() {
        assertEquals(
            ErrorType.Authorization(AuthReason.Forbidden),
            map(FirebaseFirestoreException("no", Code.PERMISSION_DENIED))
        )
        assertEquals(
            ErrorType.Authorization(AuthReason.SessionExpired),
            map(FirebaseFirestoreException("no", Code.UNAUTHENTICATED))
        )
    }

    @Test
    fun `data source rule violations map to validation reasons`() {
        assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), map(UnauthenticatedException()))
        assertEquals(ErrorType.Validation(ValidationReason.MaxProfilesReached), map(ProfileLimitReachedException(5)))
        assertEquals(ErrorType.Validation(ValidationReason.LastProfile), map(LastProfileException()))
    }

    @Test
    fun `other failures fall back to the generic mapper`() {
        assertTrue(map(IOException("io")) is ErrorType.Network)
        assertTrue(map(IllegalStateException("x")) is ErrorType.Unknown)
    }
}
