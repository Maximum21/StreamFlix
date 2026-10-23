package com.asadraza.streamflix.core.data.source.firebase

import com.asadraza.streamflix.core.data.source.LastProfileException
import com.asadraza.streamflix.core.data.source.ProfileDataSource
import com.asadraza.streamflix.core.data.source.ProfileLimitReachedException
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.model.profile.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Profiles live at /users/{uid}/profiles/{profileId}. The count on the user document is kept in
 * step with the sub-collection inside transactions, which is what makes the limit and the
 * "keep one profile" rule safe against concurrent writes.
 */
class FirestoreProfileDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProfileDataSource {
    private fun userDocument(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw UnauthenticatedException()
        return firestore.collection(COLLECTION_USERS).document(uid)
    }

    private fun profiles(): CollectionReference = userDocument().collection(COLLECTION_PROFILES)

    override fun observeProfiles(): Flow<List<Profile>> = callbackFlow {
        val registration = profiles().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toProfile() }.sortedBy { it.createdAt })
            }
        }
        awaitClose { registration.remove() }
    }

    override fun observeProfile(profileId: String): Flow<Profile?> = callbackFlow {
        val registration = profiles().document(profileId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                trySend(snapshot.takeIf { it.exists() }?.toProfile())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun create(profile: Profile): Profile {
        val user = userDocument()
        val newDocument = user.collection(COLLECTION_PROFILES).document()
        val created = profile.copy(id = newDocument.id)

        firestore.runTransaction { transaction ->
            val count = transaction.get(user).getLong(FIELD_PROFILE_COUNT) ?: 0L
            if (count >= ProfileDataSource.MAX_PROFILES) {
                throw ProfileLimitReachedException(ProfileDataSource.MAX_PROFILES)
            }
            transaction.set(newDocument, created.toFields())
            transaction.set(user, mapOf(FIELD_PROFILE_COUNT to count + 1), SetOptions.merge())
        }.await()
        return created
    }

    override suspend fun update(profile: Profile) {
        profiles().document(profile.id).set(profile.toFields(), SetOptions.merge()).await()
    }

    override suspend fun delete(profileId: String) {
        val user = userDocument()
        val target = user.collection(COLLECTION_PROFILES).document(profileId)

        firestore.runTransaction { transaction ->
            val count = transaction.get(user).getLong(FIELD_PROFILE_COUNT) ?: 0L
            if (count <= 1L) throw LastProfileException()
            transaction.delete(target)
            transaction.set(user, mapOf(FIELD_PROFILE_COUNT to count - 1), SetOptions.merge())
        }.await()
    }

    private fun DocumentSnapshot.toProfile(): Profile? {
        val name = getString("name") ?: return null
        return Profile(
            id = id,
            name = name,
            avatarUrl = getString("avatarUrl").orEmpty(),
            isKidsProfile = getBoolean("isKidsProfile") ?: false,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun Profile.toFields() = mapOf(
        "name" to name,
        "avatarUrl" to avatarUrl,
        "isKidsProfile" to isKidsProfile,
        "createdAt" to createdAt,
        "updatedAt" to System.currentTimeMillis()
    )

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_PROFILES = "profiles"
        const val FIELD_PROFILE_COUNT = "profileCount"
    }
}
