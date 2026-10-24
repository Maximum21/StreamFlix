package com.asadraza.streamflix.core.model.profile

data class Profile(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isKidsProfile: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Id of a profile that has not been stored yet; the data source assigns the real one. */
        const val NEW_ID = ""
        const val MAX_NAME_LENGTH = 20
        const val MAX_PROFILES = 5
    }
}
