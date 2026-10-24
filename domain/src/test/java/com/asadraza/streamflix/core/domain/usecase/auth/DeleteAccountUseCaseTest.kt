package com.asadraza.streamflix.core.domain.usecase.auth

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountUseCaseTest {
    private val authRepository = mockk<AuthRepository>()
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val deleteAccount = DeleteAccountUseCase(authRepository, profileRepository)

    @Test
    fun `forgets the selected profile once the account is gone`() = runTest {
        coEvery { authRepository.deleteAccount("pw") } returns flowOf(Result.Loading(), Result.Success(Unit))

        deleteAccount("pw").test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { profileRepository.clearCurrentProfile() }
    }

    @Test
    fun `keeps the selected profile when deletion fails`() = runTest {
        val error = Result.Error<Unit>(Exception(), ErrorType.Authorization(AuthReason.InvalidCredentials))
        coEvery { authRepository.deleteAccount("wrong") } returns flowOf(error)

        deleteAccount("wrong").test {
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 0) { profileRepository.clearCurrentProfile() }
    }
}
