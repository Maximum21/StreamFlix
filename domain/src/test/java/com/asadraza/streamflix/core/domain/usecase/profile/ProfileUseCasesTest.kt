package com.asadraza.streamflix.core.domain.usecase.profile

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.model.profile.Profile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUseCasesTest {
    private val repository = mockk<ProfileRepository>()
    private val saveProfile = SaveProfileUseCase(repository)

    private fun profile(name: String) = Profile(id = Profile.NEW_ID, name = name, avatarUrl = "")

    private suspend fun assertRejected(name: String, reason: ValidationReason) {
        saveProfile(profile(name)).test {
            val error = awaitItem() as Result.Error
            assertEquals(ErrorType.Validation(reason), error.type)
            awaitComplete()
        }
        verify(exactly = 0) { repository.saveProfile(any()) }
    }

    @Test
    fun `blank names are rejected before reaching the repository`() = runTest {
        assertRejected("   ", ValidationReason.NameBlank)
    }

    @Test
    fun `names over the limit are rejected`() = runTest {
        assertRejected("x".repeat(Profile.MAX_NAME_LENGTH + 1), ValidationReason.NameTooLong)
    }

    @Test
    fun `valid profiles are delegated to the repository`() = runTest {
        val valid = profile("Kids")
        every { repository.saveProfile(valid) } returns flowOf(Result.Success(Unit))

        saveProfile(valid).test {
            assertTrue(awaitItem() is Result.Success)
            awaitComplete()
        }
    }

    @Test
    fun `names at the limit are accepted`() = runTest {
        val valid = profile("x".repeat(Profile.MAX_NAME_LENGTH))
        every { repository.saveProfile(valid) } returns flowOf(Result.Success(Unit))

        saveProfile(valid).test {
            assertTrue(awaitItem() is Result.Success)
            awaitComplete()
        }
    }
}
