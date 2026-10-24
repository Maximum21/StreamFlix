package com.asadraza.streamflix.feature.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.testing.testMovie
import com.asadraza.streamflix.feature.home.components.CategoryRow
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class CategoryRowRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sourcesCreated = AtomicInteger(0)

    @After
    fun tearDown() = scope.cancel()

    // Every refresh (and the first load) creates a new PagingSource, which makes refreshes countable
    private val feed = Pager(PagingConfig(pageSize = 5, enablePlaceholders = false)) {
        sourcesCreated.incrementAndGet()
        object : PagingSource<Int, Movie>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> =
                LoadResult.Page(listOf(testMovie("1")), prevKey = null, nextKey = null)

            override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
        }
    }.flow.cachedIn(scope)

    @Test
    fun `a row refreshes once for a pull and not again when it scrolls back into view`() {
        val tick = mutableIntStateOf(0)
        lateinit var listScope: CoroutineScope
        val listState = LazyListState()

        composeRule.setContent {
            listScope = rememberCoroutineScope()
            LazyColumn(state = listState, modifier = Modifier.height(300.dp)) {
                item(key = "row") {
                    CategoryRow(title = "Row", feed = feed, refreshTick = tick.intValue, onMovieClick = {})
                }
                items(10, key = { "filler-$it" }) { Spacer(Modifier.height(300.dp)) }
            }
        }
        composeRule.waitForIdle()
        assertEquals("first load", 1, sourcesCreated.get())

        tick.intValue = 1
        composeRule.waitForIdle()
        val afterPull = sourcesCreated.get()
        assertEquals("the pull refreshed the row once", 2, afterPull)

        composeRule.runOnIdle { listScope.launch { listState.scrollToItem(8) } }
        composeRule.waitForIdle()
        composeRule.runOnIdle { listScope.launch { listState.scrollToItem(0) } }
        composeRule.waitForIdle()

        assertEquals("scrolling back must not refresh again", afterPull, sourcesCreated.get())
    }
}
