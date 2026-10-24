package com.asadraza.streamflix.core.domain.usecase.auth

import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogoutUseCaseTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)

    @Test
    fun `signs out and then forgets the selected profile`() = runTest {
        LogoutUseCase(authRepository, profileRepository)()

        coVerifyOrder {
            authRepository.logout()
            profileRepository.clearCurrentProfile()
        }
    }
}
