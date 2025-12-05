package com.asadraza.streamflix.core.data.repository

import android.util.Log
import com.asadraza.streamflix.core.common.result.ErrorMapper
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import com.asadraza.streamflix.core.model.auth.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Authentication and Firestore.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    /**
     * Login with email and password.
     *
     * Validates email verification before allowing access.
     * @throws Exception if login fails or email not verified
     */
    override suspend fun login(email: String, password: String): Flow<Result<Unit>> = flow {
        try {
            auth.signInWithEmailAndPassword(email, password).await()

            val user = auth.currentUser
            if (user != null && !user.isEmailVerified) {
                // Sign out immediately if email not verified
                auth.signOut()
                emit(Result.Error(
                    exception = Exception("Email not verified"),
                    type = ErrorType.Authorization("Please verify your email before logging in")
                ))
            }

            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sign up new user with email and password.
     * @throws Exception if any step fails
     */
    override suspend fun signUp(name: String, email: String, password: String): Flow<Result<Unit>> =
        flow {
            try {
                // Create user account
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("User creation failed")

                // Update profile with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()

                // Send verification email
                user.sendEmailVerification().await()

                // Create user document in Firestore
                val userDoc = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to name,
                    "createdAt" to System.currentTimeMillis(),
                    "emailVerified" to false
                )

                firestore.collection("users")
                    .document(user.uid)
                    .set(userDoc)
                    .await()

                emit(Result.Success(Unit))
            } catch (e: Exception) {
                emit(Result.Error(
                    exception = e,
                    type = ErrorMapper.map(e)
                ))
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Logout current user.
     * Simple operation with no error handling needed.
     */
    override suspend fun logout() {
        auth.signOut()
    }

    /**
     * Send password reset email.
     * @param email User's email address
     * @throws Exception if email sending fails
     */
    override suspend fun sendPasswordResetEmail(email: String): Flow<Result<Unit>> = flow {
        try {
            auth.sendPasswordResetEmail(email).await()
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Observe email verification status in real-time.
     *
     * @return Flow that emits EmailVerificationStatus updates
     */
    override fun observeEmailVerification(): Flow<Result<EmailVerificationStatus>> = callbackFlow {
        val user = auth.currentUser

        // Early return if no user logged in
        if (user == null) {
            trySend(Result.Error(
                exception = Exception("No user logged in"),
                type = ErrorType.Authorization("No user logged in")
            ))
            close()
            return@callbackFlow
        }

        // Firestore real-time listener for user document
        val listenerRegistration = firestore.collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                // Handle listener errors
                if (error != null) {
                    trySend(Result.Error(
                        exception = error,
                        type = ErrorMapper.map(error)
                    ))
                    return@addSnapshotListener
                }

                // Process snapshot
                if (snapshot != null && snapshot.exists()) {
                    // Reload Firebase Auth to get latest email verification status
                    user.reload().addOnSuccessListener {
                        val status = EmailVerificationStatus(
                            email = user.email ?: "",
                            isVerified = user.isEmailVerified
                        )

                        // Sync verification status to Firestore if needed
                        val firestoreVerified = snapshot.getBoolean("emailVerified") ?: false
                        if (user.isEmailVerified && !firestoreVerified) {
                            firestore.collection("users")
                                .document(user.uid)
                                .update("emailVerified", true)
                        }

                        // Emit the status
                        trySend(Result.Success(status))

                    }.addOnFailureListener { exception ->
                        trySend(Result.Error(
                            exception = exception,
                            type = ErrorMapper.map(exception)
                        ))
                    }
                }
            }

        // Cleanup: Remove Firestore listener when Flow is cancelled
        awaitClose {
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO) // Execute on IO dispatcher

    /**
     * Resend verification email to current user.
     *
     * @throws Exception if user not logged in or email sending fails
     */
    override suspend fun resendVerificationEmail(): Flow<Result<Unit>> = flow {
        try {
            val user = auth.currentUser
            user?.let{
                user.sendEmailVerification().await()
                emit(Result.Success(Unit))
            }?:{
                throw Exception("No user logged in")
            }
        } catch (e: Exception) {
            emit(Result.Error(
                exception = e,
                type = ErrorMapper.map(e)
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Observe current user authentication state.
     * @return Flow of User (null when logged out)
     */
    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString(),
                    isEmailVerified = it.isEmailVerified
                )
            }
            trySend(user)
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }.flowOn(Dispatchers.IO)
}