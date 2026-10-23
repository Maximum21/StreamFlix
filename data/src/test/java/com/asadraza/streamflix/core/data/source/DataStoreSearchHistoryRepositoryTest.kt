package com.asadraza.streamflix.core.data.source

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSearchHistoryRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository() = DataStoreSearchHistoryRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "history.preferences_pb") }
    )

    @Test
    fun `newest query comes first and repeats move to the front`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        repository.add("matrix")
        repository.add("heat")
        repository.add("MATRIX")

        assertEquals(listOf("MATRIX", "heat"), repository.history.first())
    }

    @Test
    fun `blank queries are ignored and whitespace is trimmed`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        repository.add("   ")
        repository.add("  alien \n")

        assertEquals(listOf("alien"), repository.history.first())
    }

    @Test
    fun `only the ten most recent queries are kept`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        repeat(12) { repository.add("query $it") }

        val history = repository.history.first()
        assertEquals(10, history.size)
        assertEquals("query 11", history.first())
        assertEquals("query 2", history.last())
    }

    @Test
    fun `entries can be removed individually or cleared`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        repository.add("a")
        repository.add("b")

        repository.remove("a")
        assertEquals(listOf("b"), repository.history.first())

        repository.clear()
        assertEquals(emptyList<String>(), repository.history.first())
    }
}
