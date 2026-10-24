package com.asadraza.streamflix.core.data.repository

import app.cash.turbine.test
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.data.source.FakeCurrentProfileStore
import com.asadraza.streamflix.core.data.source.FakeProfileDataSource
import com.asadraza.streamflix.core.data.source.ProfileDataSource
import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImplTest {
    private val remote = FakeProfileDataSource()
    private val store = FakeCurrentProfileStore()

    private fun TestScope.repository() = ProfileRepositoryImpl(remote, store, UnconfinedTestDispatcher(testScheduler))

    private fun newProfile(name: String = "Ana") = Profile(Profile.NEW_ID, name, avatarUrl = "")

    private suspend fun ProfileRepositoryImpl.create(name: String = "Ana") = saveProfile(newProfile(name)).first {
        it !is Result.Loading
    }

    @Test
    fun `saving a new profile creates it with a generated id`() = runTest {
        val result = repository().create()

        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf("p1"), remote.profiles.value.map { it.id })
    }

    @Test
    fun `saving an existing profile updates it`() = runTest {
        val repository = repository()
        repository.create("Ana")

        repository.saveProfile(remote.profiles.value.single().copy(name = "Anna")).first { it !is Result.Loading }

        assertEquals(listOf("Anna"), remote.profiles.value.map { it.name })
    }

    @Test
    fun `creating past the limit is a validation error`() = runTest {
        val repository = repository()
        repeat(ProfileDataSource.MAX_PROFILES) { repository.create("P$it") }

        val result = repository.create("One too many")

        assertEquals(
            ErrorType.Validation(ValidationReason.MaxProfilesReached),
            (result as Result.Error).type
        )
    }

    @Test
    fun `the last profile cannot be deleted`() = runTest {
        val repository = repository()
        repository.create()

        val result = repository.deleteProfile("p1").first { it !is Result.Loading }

        assertEquals(ErrorType.Validation(ValidationReason.LastProfile), (result as Result.Error).type)
        assertEquals(1, remote.profiles.value.size)
    }

    @Test
    fun `deleting the active profile clears the selection`() = runTest {
        val repository = repository()
        repository.create("A")
        repository.create("B")
        store.set("p1")

        repository.deleteProfile("p1").first { it !is Result.Loading }

        assertNull(store.currentProfileId.first())
    }

    @Test
    fun `deleting another profile keeps the selection`() = runTest {
        val repository = repository()
        repository.create("A")
        repository.create("B")
        store.set("p1")

        repository.deleteProfile("p2").first { it !is Result.Loading }

        assertEquals("p1", store.currentProfileId.first())
    }

    @Test
    fun `getProfiles emits loading then the list`() = runTest {
        val repository = repository()
        repository.create("A")

        repository.getProfiles().test {
            assertTrue(awaitItem() is Result.Loading)
            assertEquals(listOf("A"), (awaitItem() as Result.Success).data.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProfiles failure is reported without data`() = runTest {
        val repository = repository()
        repository.create("A")
        remote.failure = IllegalStateException("boom")

        repository.getProfiles().test {
            awaitItem()
            val error = awaitItem() as Result.Error
            assertNull(error.data)
            awaitComplete()
        }
    }

    @Test
    fun `getCurrentProfile is null until one is selected`() = runTest {
        val repository = repository()
        repository.create("A")

        repository.getCurrentProfile().test {
            assertEquals(Result.Success(null), awaitItem())
            store.set("p1")
            assertTrue(awaitItem() is Result.Loading)
            assertEquals("A", ((awaitItem() as Result.Success).data as Profile).name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearCurrentProfile forgets the selection`() = runTest {
        store.set("p1")

        repository().clearCurrentProfile()

        assertNull(store.currentProfileId.first())
    }
}
