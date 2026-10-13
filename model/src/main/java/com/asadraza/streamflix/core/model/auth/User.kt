package com.asadraza.streamflix.core.model.auth

/**
 * Domain model for User.
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean
)