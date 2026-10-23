package com.asadraza.streamflix.core.data.source.firebase

import com.asadraza.streamflix.core.data.source.AuthDataSource
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthDataSource {
    override suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toUser() ?: throw UnauthenticatedException()
    }

    override suspend fun register(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw UnauthenticatedException()

        // The account exists from here on, so a failure below must not fail the registration: a retry
        // would only report "email already in use". The display name is cosmetic, the verification
        // email can be resent from the verification screen, and the user document is completed the
        // next time the profile count is written.
        bestEffort { user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await() }
        bestEffort { user.sendEmailVerification().await() }
        bestEffort {
            firestore.collection(COLLECTION_USERS).document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to name,
                    "createdAt" to System.currentTimeMillis(),
                    "emailVerified" to false
                )
            ).await()
        }
    }

    @Suppress("SwallowedException")
    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (ignored: FirebaseException) {
            // See register: the account is already created
        }
    }

    override suspend fun signOut() = auth.signOut()

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun resendVerificationEmail() {
        val user = auth.currentUser ?: throw UnauthenticatedException()
        user.sendEmailVerification().await()
    }

    override suspend fun deleteAccount(password: String) {
        val user = auth.currentUser ?: throw UnauthenticatedException()
        val email = user.email ?: throw UnauthenticatedException()

        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        eraseUserData(user.uid)
        // The account goes last: once it is gone the rules no longer authorise the data deletes
        user.delete().await()
    }

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun observeEmailVerification(): Flow<EmailVerificationStatus> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            close(UnauthenticatedException())
            return@callbackFlow
        }

        val registration = userDocument(user).addSnapshotListener { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> launch {
                    try {
                        trySend(refreshVerification(user, snapshot.getBoolean(FIELD_EMAIL_VERIFIED) == true))
                    } catch (e: FirebaseException) {
                        close(e)
                    }
                }
            }
        }

        awaitClose { registration.remove() }
    }

    // Reloads the auth user, then mirrors a newly verified email into the user document
    private suspend fun refreshVerification(user: FirebaseUser, storedAsVerified: Boolean): EmailVerificationStatus {
        user.reload().await()
        if (user.isEmailVerified && !storedAsVerified) {
            // Merge instead of update so a legacy document without a uid is completed, as the rules require
            userDocument(user).set(mapOf("uid" to user.uid, FIELD_EMAIL_VERIFIED to true), SetOptions.merge())
        }
        return EmailVerificationStatus(email = user.email.orEmpty(), isVerified = user.isEmailVerified)
    }

    private suspend fun eraseUserData(uid: String) {
        val userDocument = firestore.collection(FirestorePaths.USERS).document(uid)
        val profiles = userDocument.collection(FirestorePaths.PROFILES).get().await().documents

        profiles.forEach { firestore.eraseProfileData(it.reference) }

        // Profiles and the user document go together: the rules only let the last profile be
        // deleted in the same write that deletes the user document
        firestore.runBatch { batch ->
            profiles.forEach { batch.delete(it.reference) }
            batch.delete(userDocument)
        }.await()
    }

    private fun userDocument(user: FirebaseUser) = firestore.collection(COLLECTION_USERS).document(user.uid)

    private fun FirebaseUser.toUser() = User(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        isEmailVerified = isEmailVerified
    )

    private companion object {
        const val COLLECTION_USERS = FirestorePaths.USERS
        const val FIELD_EMAIL_VERIFIED = "emailVerified"
    }
}
