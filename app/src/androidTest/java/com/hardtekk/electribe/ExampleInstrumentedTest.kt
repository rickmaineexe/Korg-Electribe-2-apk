package com.hardtekk.electribe

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](https://developer.android.com/training/testing/unit-testing/instrumented-unit-tests).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext: Context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.hardtekk.electribe", appContext.packageName)
    }

    @Test
    fun testResourcesExist() {
        val appContext: Context = ApplicationProvider.getApplicationContext<Context>()
        val resources = appContext.resources

        // Test strings
        assertTrue(resources.getIdentifier("app_name", "string", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("bpm_label", "string", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("play", "string", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("stop", "string", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("rec", "string", appContext.packageName) > 0)

        // Test colors
        assertTrue(resources.getIdentifier("neon_pink", "color", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("anthracite", "color", appContext.packageName) > 0)
        assertTrue(resources.getIdentifier("led_green", "color", appContext.packageName) > 0)

        // Test drawables
        assertTrue(resources.getIdentifier("ic_launcher_foreground", "drawable", appContext.packageName) > 0)
    }

    @Test
    fun testThemeExists() {
        val appContext: Context = ApplicationProvider.getApplicationContext<Context>()
        val themeResId = appContext.resources.getIdentifier("Theme_HardtekkElectribe", "style", appContext.packageName)
        assertTrue(themeResId > 0)
    }
}