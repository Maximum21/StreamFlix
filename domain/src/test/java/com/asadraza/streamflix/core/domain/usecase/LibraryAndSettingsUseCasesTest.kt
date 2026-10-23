package com.asadraza.streamflix.core.domain.usecase

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.repository.CacheRepository
import com.asadraza.streamflix.core.domain.repository.MyListRepository
import com.asadraza.streamflix.core.domain.repository.SearchHistoryRepository
import com.asadraza.streamflix.core.domain.repository.SettingsRepository
import com.asadraza.streamflix.core.domain.repository.WatchHistoryRepository
import com.asadraza.streamflix.core.domain.usecase.library.AddToMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveIsInMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.ObserveWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RecordWatchUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromMyListUseCase
import com.asadraza.streamflix.core.domain.usecase.library.RemoveFromWatchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.AddSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ClearSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.ObserveSearchHistoryUseCase
import com.asadraza.streamflix.core.domain.usecase.search.RemoveSearchQueryUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ClearCacheUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetAnalyticsConsentUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.SetThemeModeUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie
import com.asadraza.streamflix.core.model.settings.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** These use cases forward to a repository; the tests pin down that each forwards its arguments. */
class LibraryAndSettingsUseCasesTest {
    private val unitResult: Flow<Result<Unit>> = flowOf(Result.Success(Unit))
    private val item = MyListItem("1", "Title", null, 1)
    private val watched = WatchedMovie("1", "Title", null, null, 1, 5)

    @Test
    fun `my list use cases forward to the repository`() {
        val repository = mockk<MyListRepository>()
        val list = flowOf(Result.Success(listOf(item)))
        val membership = flowOf(true)
        every { repository.observeMyList() } returns list
        every { repository.observeIsInMyList("1") } returns membership
        every { repository.add(item) } returns unitResult
        every { repository.remove("1") } returns unitResult

        assertSame(list, ObserveMyListUseCase(repository)())
        assertSame(membership, ObserveIsInMyListUseCase(repository)("1"))
        assertSame(unitResult, AddToMyListUseCase(repository)(item))
        assertSame(unitResult, RemoveFromMyListUseCase(repository)("1"))
    }

    @Test
    fun `watch history use cases forward to the repository`() = runTest {
        val repository = mockk<WatchHistoryRepository>()
        val recent = flowOf(Result.Success(listOf(watched)))
        every { repository.observeRecent() } returns recent
        coEvery { repository.record(watched) } returns Result.Success(Unit)
        every { repository.remove("1") } returns unitResult

        assertSame(recent, ObserveWatchHistoryUseCase(repository)())
        assertEquals(Result.Success(Unit), RecordWatchUseCase(repository)(watched))
        coVerify { repository.record(watched) }
        assertSame(unitResult, RemoveFromWatchHistoryUseCase(repository)("1"))
    }

    @Test
    fun `search history use cases forward to the repository`() = runTest {
        val repository = mockk<SearchHistoryRepository>(relaxed = true)
        val history = flowOf(listOf("heat"))
        every { repository.history } returns history

        assertSame(history, ObserveSearchHistoryUseCase(repository)())
        AddSearchQueryUseCase(repository)("heat")
        RemoveSearchQueryUseCase(repository)("heat")
        ClearSearchHistoryUseCase(repository)()

        coVerify {
            repository.add("heat")
            repository.remove("heat")
            repository.clear()
        }
    }

    @Test
    fun `settings use cases forward to the repositories`() = runTest {
        val settings = mockk<SettingsRepository>(relaxed = true)
        val cache = mockk<CacheRepository>(relaxed = true)
        val consent = flowOf<Boolean?>(null)
        every { settings.analyticsConsent } returns consent
        val theme = flowOf(ThemeMode.Dark)
        every { settings.themeMode } returns theme

        assertSame(consent, ObserveAnalyticsConsentUseCase(settings)())
        assertSame(theme, ObserveThemeModeUseCase(settings)())
        SetAnalyticsConsentUseCase(settings)(true)
        SetThemeModeUseCase(settings)(ThemeMode.Light)
        ClearCacheUseCase(cache)()

        coVerify {
            settings.setAnalyticsConsent(true)
            settings.setThemeMode(ThemeMode.Light)
            cache.clearCache()
        }
    }
}
