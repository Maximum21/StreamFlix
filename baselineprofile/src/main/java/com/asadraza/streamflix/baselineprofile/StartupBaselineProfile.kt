package com.asadraza.streamflix.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the classes and methods used while starting the app and scrolling Home.
 *
 * Without credentials it profiles cold start and the sign-in screen. To include Home, pass a test
 * account of your own (never commit it):
 * the instrumentation arguments `testEmail` and `testPassword`, for example
 * `-Pandroid.testInstrumentationRunnerArguments.testEmail=you@example.com`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBaselineProfile {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val arguments = InstrumentationRegistry.getArguments()
        val packageName = requireNotNull(arguments.getString("targetAppId")) { "targetAppId was not provided" }
        val email = arguments.getString("testEmail")
        val password = arguments.getString("testPassword")

        rule.collect(packageName = packageName, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()

            if (email != null && password != null) {
                val fields = device.findObjects(By.clazz("android.widget.EditText"))
                if (fields.size >= 2) {
                    fields[0].text = email
                    fields[1].text = password
                    device.findObject(By.text("Login"))?.click()
                    // First profile, then Home
                    device.wait(Until.hasObject(By.textContains("watching")), TIMEOUT_MILLIS)
                    device.findObjects(By.clickable(true)).firstOrNull()?.click()
                    device.wait(Until.hasObject(By.text("Trending Now")), TIMEOUT_MILLIS)
                    device.findObject(By.scrollable(true))?.fling(androidx.test.uiautomator.Direction.DOWN)
                    device.waitForIdle()
                }
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
