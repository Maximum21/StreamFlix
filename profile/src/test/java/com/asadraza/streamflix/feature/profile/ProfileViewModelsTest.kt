package com.asadraza.streamflix.feature.profile

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.domain.usecase.profile.DeleteProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SaveProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SetCurrentProfileUseCase
import com.asadraza.streamflix.core.model.profile.Profile
import com.asadraza.streamflix.core.testing.MainDispatcherRule
import com.asadraza.streamflix.feature.profile.management.ProfileManagementEffect
import com.asadraza.streamflix.feature.profile.management.ProfileManagementEvent
import com.asadraza.streamflix.feature.profile.management.ProfileManagementViewModel
import com.asadraza.streamflix.feature.profile.selection.ProfileSelectionEffect
import com.asadraza.streamflix.feature.profile.selection.ProfileSelectionEvent
import com.asadraza.streamflix.feature.profile.selection.ProfileSelectionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val getProfiles = mockk<GetProfilesUseCase>()
    private val saveProfile = mockk<SaveProfileUseCase>()
    private val deleteProfile = mockk<DeleteProfileUseCase>()
    private val setCurrentProfile = mockk<SetCurrentProfileUseCase>()

    private fun profile(id: String) = Profile(id = id, name = "P$id", avatarUrl = "")

    private fun profiles(count: Int) = List(count) { profile("$it") }

    private fun management(list: List<Profile>): ProfileManagementViewModel {
        every { getProfiles() } returns flowOf(Result.Success(list))
        return ProfileManagementViewModel(getProfiles, saveProfile, deleteProfile)
    }

    private fun selection(list: List<Profile>): ProfileSelectionViewModel {
        every { getProfiles() } returns flowOf(Result.Success(list))
        return ProfileSelectionViewModel(getProfiles, setCurrentProfile)
    }

    @Test
    fun `management blocks adding beyond the profile limit`() = runTest {
        val viewModel = management(profiles(Profile.MAX_PROFILES))
        assertTrue(viewModel.state.value.maxProfilesReached)

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnAddProfileClick)
            assertTrue(awaitItem() is ProfileManagementEffect.ShowToast)
        }
        assertFalse(viewModel.state.value.showCreateDialog)
    }

    @Test
    fun `management opens the create dialog below the limit`() {
        val viewModel = management(profiles(2))

        viewModel.onEvent(ProfileManagementEvent.OnAddProfileClick)

        assertTrue(viewModel.state.value.showCreateDialog)
    }

    @Test
    fun `the last profile cannot be deleted`() = runTest {
        val viewModel = management(profiles(1))
        assertFalse(viewModel.state.value.canDeleteProfile)

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnDeleteProfileClick(profile("0")))
            assertTrue(awaitItem() is ProfileManagementEffect.ShowToast)
        }
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `confirming a delete removes the profile and closes the dialog`() = runTest {
        coEvery { deleteProfile("1") } returns flowOf(Result.Success(Unit))
        val viewModel = management(profiles(3))
        viewModel.onEvent(ProfileManagementEvent.OnDeleteProfileClick(profile("1")))
        assertTrue(viewModel.state.value.showDeleteDialog)

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnDeleteConfirm)
            assertTrue(awaitItem() is ProfileManagementEffect.ShowToast)
        }
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `a new profile is saved with the new id and a trimmed name`() = runTest {
        val saved = slot<Profile>()
        every { saveProfile(capture(saved)) } returns flowOf(Result.Success(Unit))
        val viewModel = management(profiles(1))
        viewModel.onEvent(ProfileManagementEvent.OnAddProfileClick)

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnSaveProfile(name = "  Kids  ", isKidsProfile = true))
            assertTrue(awaitItem() is ProfileManagementEffect.ShowToast)
        }

        assertEquals(Profile.NEW_ID, saved.captured.id)
        assertEquals("Kids", saved.captured.name)
        assertTrue(saved.captured.isKidsProfile)
        assertFalse(viewModel.state.value.showCreateDialog)
    }

    @Test
    fun `editing keeps the profile id`() = runTest {
        val saved = slot<Profile>()
        every { saveProfile(capture(saved)) } returns flowOf(Result.Success(Unit))
        val viewModel = management(profiles(2))
        viewModel.onEvent(ProfileManagementEvent.OnEditProfileClick(profile("1")))

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnSaveProfile(name = "Renamed"))
            awaitItem()
        }

        assertEquals("1", saved.captured.id)
        assertEquals("Renamed", saved.captured.name)
    }

    @Test
    fun `a rejected save keeps the dialog open and reports the reason`() = runTest {
        val error = Result.Error<Unit>(Exception(), ErrorType.Validation(ValidationReason.NameBlank))
        every { saveProfile(any()) } returns flowOf(error)
        val viewModel = management(profiles(1))
        viewModel.onEvent(ProfileManagementEvent.OnAddProfileClick)

        viewModel.effect.test {
            viewModel.onEvent(ProfileManagementEvent.OnSaveProfile(name = ""))
            assertTrue(awaitItem() is ProfileManagementEffect.ShowToast)
        }
        assertTrue(viewModel.state.value.showCreateDialog)
    }

    @Test
    fun `selecting a profile stores it and navigates home`() = runTest {
        every { setCurrentProfile("1") } returns flowOf(Result.Success(Unit))
        val viewModel = selection(profiles(2))

        viewModel.effect.test {
            viewModel.onEvent(ProfileSelectionEvent.OnProfileClick(profile("1")))
            assertEquals(ProfileSelectionEffect.NavigateToHome("1"), awaitItem())
        }
    }

    @Test
    fun `a failed selection stays on the screen with a message`() = runTest {
        every { setCurrentProfile("1") } returns flowOf(Result.Error(Exception(), ErrorType.Unknown(null)))
        val viewModel = selection(profiles(2))

        viewModel.effect.test {
            viewModel.onEvent(ProfileSelectionEvent.OnProfileClick(profile("1")))
            assertTrue(awaitItem() is ProfileSelectionEffect.ShowToast)
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `the create-first-profile prompt appears once even if the empty list is reported twice`() = runTest {
        val updates = MutableSharedFlow<Result<List<Profile>>>(extraBufferCapacity = 4)
        every { getProfiles() } returns updates
        val viewModel = ProfileSelectionViewModel(getProfiles, setCurrentProfile)

        viewModel.effect.test {
            // Firestore reports the same empty list from the cache and then from the server
            updates.emit(Result.Success(emptyList()))
            updates.emit(Result.Success(emptyList()))

            assertTrue(awaitItem() is ProfileSelectionEffect.ShowToast)
            assertEquals(ProfileSelectionEffect.NavigateToManagement, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `retrying replaces the running collector instead of adding another`() = runTest {
        val first = MutableSharedFlow<Result<List<Profile>>>(extraBufferCapacity = 4)
        val second = MutableSharedFlow<Result<List<Profile>>>(extraBufferCapacity = 4)
        every { getProfiles() } returnsMany listOf(first, second)
        val viewModel = ProfileSelectionViewModel(getProfiles, setCurrentProfile)

        viewModel.onEvent(ProfileSelectionEvent.OnRetry)
        first.emit(Result.Success(profiles(3)))
        assertTrue(viewModel.state.value.profiles.isEmpty())

        second.emit(Result.Success(profiles(2)))
        assertEquals(2, viewModel.state.value.profiles.size)
    }

    @Test
    fun `no profiles sends the user to management to create one`() = runTest {
        every { getProfiles() } returns flowOf(Result.Success(emptyList()))

        // The effects are sent after construction begins collecting, so subscribe lazily
        val viewModel = ProfileSelectionViewModel(getProfiles, setCurrentProfile)

        viewModel.effect.test {
            assertTrue(awaitItem() is ProfileSelectionEffect.ShowToast)
            assertEquals(ProfileSelectionEffect.NavigateToManagement, awaitItem())
        }
        assertFalse(viewModel.state.value.hasProfiles)
    }
}
