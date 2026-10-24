// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue

/** Cambia la configurazione reale esclusivamente nell'emulatore e ripristina il valore letto. */
internal fun withEmulatorFontScale(scale: Float, block: () -> Unit) {
    assumeTrue("Questa prova modifica font_scale soltanto nell'emulatore Android",
        Build.HARDWARE in setOf("ranchu", "goldfish"))
    require(scale in 0.5f..2f)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText().trim() }
    val initialSetting = shell("settings get system font_scale")
    val initialScale = initialSetting.toFloatOrNull() ?: 1f
    fun awaitConfiguration(expected: Float) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (abs(instrumentation.targetContext.resources.configuration.fontScale - expected) > 0.01f &&
            SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
        }
        assertTrue("La configurazione Android non ha applicato font_scale=$expected",
            abs(instrumentation.targetContext.resources.configuration.fontScale - expected) <= 0.01f)
        instrumentation.waitForIdleSync()
        // Consente il completamento della ricreazione dell'Activity prima di setContent.
        SystemClock.sleep(750)
    }
    try {
        shell("settings put system font_scale $scale")
        awaitConfiguration(scale)
        block()
    } finally {
        if (initialSetting == "null") shell("settings delete system font_scale")
        else shell("settings put system font_scale $initialScale")
        awaitConfiguration(initialScale)
    }
}
