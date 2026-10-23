package com.asadraza.streamflix.feature.mylist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.asadraza.streamflix.core.model.library.MyListItem
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

        composeRule.onNodeWithContentDescription("Heat").performClick()

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
}
