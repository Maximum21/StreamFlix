package com.asadraza.streamflix.core.data.source.firebase

/** Collection names shared by the Firestore adapters. Per-profile data lives under the profile. */
internal object FirestorePaths {
    const val USERS = "users"
    const val PROFILES = "profiles"
    const val MY_LIST = "list"
    const val HISTORY = "history"

    /** Sub-collections that hang off a profile and must be erased with it. */
    val PROFILE_SUBCOLLECTIONS = listOf(MY_LIST, HISTORY)
}
