package com.asadraza.streamflix.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the message and reports retry clicks`() {
        var retries = 0
        composeRule.setContent { ErrorMessage(message = "Something broke", onRetry = { retries++ }) }

        composeRule.onNodeWithText("Something broke").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }
}
