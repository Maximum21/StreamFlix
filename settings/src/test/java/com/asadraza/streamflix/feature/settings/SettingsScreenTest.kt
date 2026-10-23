package com.asadraza.streamflix.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.asadraza.streamflix.core.model.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1600dp")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsEvent>()

    private fun setContent(state: SettingsState = SettingsState(email = "me@example.com")) {
        composeRule.setContent {
            SettingsContent(
                state = state,
                appVersion = "1.2.3",
                privacyPolicyUrl = "https://example.com/privacy",
                onEvent = { events += it }
            )
        }
    }

    @Test
    fun `shows the account, version and attribution`() {
        setContent()

        composeRule.onNodeWithText("me@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.2.3").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This product uses the TMDB API but is not endorsed or certified by TMDB."
        ).assertIsDisplayed()
    }

    @Test
    fun `rows report their events`() {
        setContent()

        composeRule.onNodeWithText("Sign out").performClick()
        composeRule.onNodeWithText("Clear cache").performClick()
        composeRule.onNodeWithText("Delete account").performClick()

        assertEquals(
            listOf(SettingsEvent.OnSignOutClick, SettingsEvent.OnClearCacheClick, SettingsEvent.OnDeleteAccountClick),
            events
        )
    }

    @Test
    fun `the theme row shows the current mode and offers all three`() {
        setContent(SettingsState(email = "me@example.com", themeMode = ThemeMode.Dark))

        composeRule.onNodeWithText("Dark").assertIsDisplayed()
        composeRule.onNodeWithText("Theme").performClick()

        composeRule.onNodeWithText("Match system").assertIsDisplayed()
        composeRule.onNodeWithText("Light").performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OnThemeSelect(ThemeMode.Light)), events)
    }
}
