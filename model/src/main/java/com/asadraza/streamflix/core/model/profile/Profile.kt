package com.asadraza.streamflix.core.model.profile

data class Profile(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isKidsProfile: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)