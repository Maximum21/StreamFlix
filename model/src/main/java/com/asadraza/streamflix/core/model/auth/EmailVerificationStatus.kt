package com.asadraza.streamflix.core.model.auth

data class EmailVerificationStatus(
    val email: String,
    val isVerified: Boolean
)