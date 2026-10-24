package com.asadraza.streamflix.core.domain.usecase

import androidx.paging.PagingData
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.domain.usecase.auth.CheckEmailVerificationUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.GetCurrentUserUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.ResendVerificationEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.asadraza.streamflix.core.domain.usecase.auth.SignUpUseCase
import com.asadraza.streamflix.core.domain.usecase.detail.GetMovieDetailsUseCase
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryPaginatedUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.DeleteProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetCurrentProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SetCurrentProfileUseCase
import com.asadraza.streamflix.core.model.movie.MovieCategory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

/** These use cases only forward to a repository; the test pins down that each forwards its arguments. */
class PassThroughUseCasesTest {
    private val auth = mockk<AuthRepository>()
    private val profiles = mockk<ProfileRepository>()
    private val catalog = mockk<MovieCatalogRepository>()
    private val details = mockk<MovieDetailRepository>()
    private val unitResult: Flow<Result<Unit>> = flowOf(Result.Success(Unit))

    @Test
    fun `auth use cases forward to the auth repository`() = runTest {
        coEvery { auth.login("a@b.com", "pw") } returns unitResult
        coEvery { auth.signUp("n", "a@b.com", "pw") } returns unitResult
        coEvery { auth.sendPasswordResetEmail("a@b.com") } returns unitResult
        coEvery { auth.resendVerificationEmail() } returns unitResult
        every { auth.observeEmailVerification() } returns flowOf()
        every { auth.getCurrentUser() } returns flowOf(null)

        assertSame(unitResult, LoginUseCase(auth)("a@b.com", "pw"))
        assertSame(unitResult, SignUpUseCase(auth)("n", "a@b.com", "pw"))
        assertSame(unitResult, SendPasswordResetEmailUseCase(auth)("a@b.com"))
        assertSame(unitResult, ResendVerificationEmailUseCase(auth)())
        CheckEmailVerificationUseCase(auth)()
        GetCurrentUserUseCase(auth)()
    }

    @Test
    fun `profile use cases forward to the profile repository`() {
        every { profiles.deleteProfile("1") } returns unitResult
        every { profiles.setCurrentProfile("1") } returns unitResult
        every { profiles.getProfiles() } returns flowOf(Result.Success(emptyList()))
        every { profiles.getCurrentProfile() } returns flowOf(Result.Success(null))

        assertSame(unitResult, DeleteProfileUseCase(profiles)("1"))
        assertSame(unitResult, SetCurrentProfileUseCase(profiles)("1"))
        GetProfilesUseCase(profiles)()
        GetCurrentProfileUseCase(profiles)()
    }

    @Test
    fun `movie use cases forward to the repositories`() = runTest {
        val paging = flowOf(PagingData.empty<com.asadraza.streamflix.core.model.movie.Movie>())
        every { catalog.getMoviesByCategoryPaginated(MovieCategory.Trending) } returns paging
        coEvery { details.getMovieDetails("9") } returns flowOf()

        assertSame(paging, GetMoviesByCategoryPaginatedUseCase(catalog)(MovieCategory.Trending))
        GetMovieDetailsUseCase(details)("9")
    }
}
