package com.asadraza.streamflix.core.data.source.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Collection names shared by the Firestore adapters. Per-profile data lives under the profile. */
internal object FirestorePaths {
    const val USERS = "users"
    const val PROFILES = "profiles"
    const val MY_LIST = "list"
    const val HISTORY = "history"

    /** Sub-collections that hang off a profile and must be erased with it. */
    val PROFILE_SUBCOLLECTIONS = listOf(MY_LIST, HISTORY)
}

private const val BATCH_LIMIT = 400

/**
 * Deletes everything stored under one profile (its list and history). Firestore does not delete
 * sub-collections together with their parent document, so they are erased explicitly, in batches
 * below the 500-write limit. The profile document itself is not touched.
 */
internal suspend fun FirebaseFirestore.eraseProfileData(profile: DocumentReference) {
    for (name in FirestorePaths.PROFILE_SUBCOLLECTIONS) {
        profile.collection(name).get().await().documents
            .chunked(BATCH_LIMIT)
            .forEach { chunk -> runBatch { batch -> chunk.forEach { batch.delete(it.reference) } }.await() }
    }
}
