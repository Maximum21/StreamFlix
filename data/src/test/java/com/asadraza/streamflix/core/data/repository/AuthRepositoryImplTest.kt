package com.asadraza.streamflix.core.data.repository

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.AuthReason
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.source.FakeAuthDataSource
import com.asadraza.streamflix.core.data.source.UnauthenticatedException
import com.asadraza.streamflix.core.data.source.testUser
import com.asadraza.streamflix.core.model.auth.EmailVerificationStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {
    private val dataSource = FakeAuthDataSource()

    private fun TestScope.repository() = AuthRepositoryImpl(dataSource, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `login succeeds for a verified user`() = runTest {
        repository().login("a@b.c", "pw").test {
            assertTrue(awaitItem() is Result.Loading)
            assertEquals(Result.Success(Unit), awaitItem())
            awaitComplete()
        }
        assertEquals(0, dataSource.signedOut)
    }

    @Test
    fun `login signs an unverified user out and reports it`() = runTest {
        dataSource.signInResult = kotlin.Result.success(testUser(verified = false))

        repository().login("a@b.c", "pw").test {
            awaitItem()
            val error = awaitItem() as Result.Error
            assertEquals(ErrorType.Authorization(AuthReason.EmailNotVerified), error.type)
            awaitComplete()
        }
        assertEquals(1, dataSource.signedOut)
    }

    @Test
    fun `login maps provider failures`() = runTest {
        dataSource.failure = UnauthenticatedException()

        repository().login("a@b.c", "pw").test {
            awaitItem()
            val error = awaitItem() as Result.Error
            assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), error.type)
            awaitComplete()
        }
    }

    @Test
    fun `signUp registers through the data source`() = runTest {
        repository().signUp("Name", "a@b.c", "pw").test {
            awaitItem()
            assertEquals(Result.Success(Unit), awaitItem())
            awaitComplete()
        }
        assertEquals(Triple("Name", "a@b.c", "pw"), dataSource.registered)
    }

    @Test
    fun `resendVerificationEmail reports a missing user instead of finishing silently`() = runTest {
        dataSource.failure = UnauthenticatedException()

        repository().resendVerificationEmail().test {
            awaitItem()
            assertTrue(awaitItem() is Result.Error)
            awaitComplete()
        }
    }

    @Test
    fun `resendVerificationEmail succeeds for a signed in user`() = runTest {
        repository().resendVerificationEmail().test {
            awaitItem()
            assertEquals(Result.Success(Unit), awaitItem())
            awaitComplete()
        }
        assertEquals(1, dataSource.verificationEmailsSent)
    }

    @Test
    fun `observeEmailVerification wraps status updates`() = runTest {
        val status = EmailVerificationStatus("a@b.c", isVerified = true)
        dataSource.verificationFlow = flowOf(status)

        repository().observeEmailVerification().test {
            assertEquals(Result.Success(status), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeEmailVerification turns a stream failure into an error result`() = runTest {
        repository().observeEmailVerification().test {
            val error = awaitItem() as Result.Error
            assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), error.type)
            awaitComplete()
        }
    }

    @Test
    fun `getCurrentUser follows the data source`() = runTest {
        repository().getCurrentUser().test {
            assertEquals(null, awaitItem())
            dataSource.users.value = testUser()
            assertEquals(testUser(), awaitItem())
        }
    }

    @Test
    fun `deleting the account passes the password to the provider`() = runTest {
        repository().deleteAccount("secret").test {
            assertTrue(awaitItem() is Result.Loading)
            assertEquals(Result.Success(Unit), awaitItem())
            awaitComplete()
        }
        assertEquals("secret", dataSource.deletedWithPassword)
    }

    @Test
    fun `a failed deletion is reported and nothing is recorded`() = runTest {
        dataSource.failure = UnauthenticatedException()

        repository().deleteAccount("secret").test {
            awaitItem()
            val error = awaitItem() as Result.Error
            assertEquals(ErrorType.Authorization(AuthReason.NotSignedIn), error.type)
            awaitComplete()
        }
        assertEquals(null, dataSource.deletedWithPassword)
    }
}
