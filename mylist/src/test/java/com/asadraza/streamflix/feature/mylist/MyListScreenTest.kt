package com.asadraza.streamflix.feature.mylist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.library.ObserveMyListUseCase
import com.asadraza.streamflix.core.model.library.MyListItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class MyListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<MyListEvent>()

    private fun setContent(state: MyListState) {
        composeRule.setContent { MyListContent(state = state, onEvent = { events += it }) }
    }

    @Test
    fun `an empty list explains how to add titles`() {
        setContent(MyListState(isLoading = false))

        composeRule.onNodeWithText("Your list is empty. Add titles from their detail page.").assertIsDisplayed()
    }

    @Test
    fun `saved titles are shown and open on click`() {
        setContent(MyListState(items = listOf(MyListItem("7", "Heat", null, 1)), isLoading = false))

        composeRule.onNodeWithText("Heat").performClick()

        assertEquals(listOf<MyListEvent>(MyListEvent.OnItemClick("7")), events)
    }

    @Test
    fun `a failed first load offers a retry`() {
        setContent(
            MyListState(
                isLoading = false,
                errorType = com.asadraza.streamflix.core.common.result.ErrorType.Http(500)
            )
        )

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(listOf<MyListEvent>(MyListEvent.OnRetry), events)
    }

    @Test
    fun `a failed refresh with titles on screen is reported without hiding them`() {
        setContent(
            MyListState(
                items = listOf(MyListItem("7", "Heat", null, 1)),
                isLoading = false,
                errorType = com.asadraza.streamflix.core.common.result.ErrorType.Http(500)
            )
        )

        composeRule.onNodeWithText("Server error. Please try again later").assertIsDisplayed()
        composeRule.onNodeWithText("Heat").assertIsDisplayed()
    }

    @Test
    fun `the screen has its own top bar so the list is not drawn under the status bar`() {
        val observeMyList = mockk<ObserveMyListUseCase>()
        every { observeMyList() } returns flowOf(Result.Success(listOf(MyListItem("7", "Heat", null, 1))))
        val viewModel = MyListViewModel(observeMyList)

        composeRule.setContent { MyListScreen(onMovieClick = {}, viewModel = viewModel) }

        composeRule.onNodeWithText("My List").assertIsDisplayed()
        composeRule.onNodeWithText("Heat").assertIsDisplayed()
    }
}
