package com.asadraza.streamflix.core.domain.usecase.auth

import javax.inject.Inject

/**
 * Use case for name validation.
 */
class ValidateNameUseCase @Inject constructor() {
    operator fun invoke(name: String): Result<Unit> {
        return when {
            name.isBlank() -> {
                Result.failure(Exception("Name cannot be empty"))
            }
            name.length < 2 -> {
                Result.failure(Exception("Name must be at least 2 characters"))
            }
            name.length > 50 -> {
                Result.failure(Exception("Name must be less than 50 characters"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
}
