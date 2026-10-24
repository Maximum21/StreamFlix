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
import com.google.firebase.firestore.Transaction
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

        reconcileCount(user)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(user)
            val count = snapshot.getLong(FIELD_PROFILE_COUNT) ?: 0L
            if (count >= ProfileDataSource.MAX_PROFILES) {
                throw ProfileLimitReachedException(ProfileDataSource.MAX_PROFILES)
            }
            transaction.set(newDocument, created.toFields())
            writeUserCount(transaction, user, snapshot.exists(), count + 1)
        }.await()
        return created
    }

    override suspend fun update(profile: Profile) {
        profiles().document(profile.id).set(profile.toFields(), SetOptions.merge()).await()
    }

    override suspend fun delete(profileId: String) {
        val user = userDocument()
        val target = user.collection(COLLECTION_PROFILES).document(profileId)

        reconcileCount(user)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(user)
            val count = snapshot.getLong(FIELD_PROFILE_COUNT) ?: 0L
            if (count <= 1L) throw LastProfileException()
            transaction.delete(target)
            writeUserCount(transaction, user, snapshot.exists(), count - 1)
        }.await()
        // After the transaction, so a refused delete (the last profile) never loses its data. The rules
        // allow the owner to delete these documents whether or not the profile still exists.
        firestore.eraseProfileData(target)
    }

    /**
     * Profiles created before the counter existed leave it missing or wrong, which would make the
     * limit and the "keep one profile" rule act on a stale number. Transactions cannot run queries,
     * so the stored count is aligned with the real number of profiles just before each change.
     */
    private suspend fun reconcileCount(user: DocumentReference) {
        val actual = user.collection(COLLECTION_PROFILES).get().await().size().toLong()
            .coerceAtMost(ProfileDataSource.MAX_PROFILES.toLong())
        val snapshot = user.get().await()
        if (snapshot.exists() && snapshot.getLong(FIELD_PROFILE_COUNT) == actual) return

        val fields = userFields(user.id, snapshot.exists(), actual)
        val write = if (snapshot.exists()) user.set(fields, SetOptions.merge()) else user.set(fields)
        write.await()
    }

    private fun writeUserCount(transaction: Transaction, user: DocumentReference, exists: Boolean, count: Long) {
        val fields = userFields(user.id, exists, count)
        if (exists) transaction.set(user, fields, SetOptions.merge()) else transaction.set(user, fields)
    }

    /**
     * The uid is written with every change because the rules require it to match the document id,
     * which also completes documents created before it was stored. A missing document is created
     * in full so it satisfies the create rule.
     */
    private fun userFields(uid: String, exists: Boolean, count: Long): Map<String, Any?> {
        if (exists) return mapOf(FIELD_UID to uid, FIELD_PROFILE_COUNT to count)
        val account = auth.currentUser
        return mapOf(
            FIELD_UID to uid,
            "email" to account?.email.orEmpty(),
            "displayName" to account?.displayName,
            "createdAt" to System.currentTimeMillis(),
            "emailVerified" to (account?.isEmailVerified == true),
            FIELD_PROFILE_COUNT to count
        )
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
        const val FIELD_UID = "uid"
    }
}
