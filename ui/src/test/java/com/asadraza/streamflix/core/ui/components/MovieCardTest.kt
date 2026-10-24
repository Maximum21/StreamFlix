package com.asadraza.streamflix.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp")
class MovieCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a title without a poster shows its name instead of an empty box`() {
        var clicks = 0
        composeRule.setContent { MovieCard(posterUrl = null, title = "Heat", onClick = { clicks++ }) }

        composeRule.onNodeWithText("Heat").assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `the placeholder card is available for items that have not loaded`() {
        composeRule.setContent { MovieCardPlaceholder() }

        composeRule.onNodeWithTag(MOVIE_CARD_PLACEHOLDER_TAG).assertIsDisplayed()
    }
}
