// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import android.app.LocaleManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import it.sorguido.chromecastkillswitch.core.DesiredState
import it.sorguido.chromecastkillswitch.core.Identity
import it.sorguido.chromecastkillswitch.core.OperationStage
import it.sorguido.chromecastkillswitch.core.PendingOperation
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Emulator-only system configuration exercise; restores both system and per-app languages.
 * Android's test emulator must temporarily permit hidden API access for the configuration setter.
 * No such API or test fixture is included in the release APK.
 */
class SystemLanguageTest {
    @Test fun realSystemPrimaryLanguageOverridesSecondaryAndAppPreferencesAndSurvivesRecreation() {
        assumeTrue(Build.HARDWARE in setOf("ranchu", "goldfish") && Build.VERSION.SDK_INT >= 33)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(LocaleManager::class.java)
        val originalSystem = manager.systemLocales
        val originalApp = manager.applicationLocales
        val files = listOf("target.json", "pairing.json", "pending.json").map { File(context.noBackupFilesDir, it) }
        val originals = files.associateWith { if (it.exists()) it.readBytes() else null }
        val store = PrivateStore(context)
        val identity = Identity("synthetic-language-test", "Synthetic Chromecast", "boreal", 34, "14")
        val pending = PendingOperation(identity.serial, DesiredState.ON,
            "00000000-0000-4000-8000-000000000001", OperationStage.REBOOT_REQUESTED)
        fun setSystem(tags: String) {
            val configuration = Configuration(Resources.getSystem().configuration).apply {
                setLocales(LocaleList.forLanguageTags(tags))
            }
            instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.CHANGE_CONFIGURATION", "android.permission.WRITE_SETTINGS")
            try {
                val service = Class.forName("android.app.ActivityManager").getDeclaredMethod("getService").invoke(null)
                Class.forName("android.app.IActivityManager")
                    .getMethod("updatePersistentConfiguration", Configuration::class.java).invoke(service, configuration)
            } finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (manager.systemLocales.toLanguageTags() != tags && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
            assertEquals(tags, manager.systemLocales.toLanguageTags())
            instrumentation.waitForIdleSync()
        }
        try {
            store.saveIdentity(identity)
            store.savePairing("192.0.2.1")
            store.write(pending)
            for ((system, app, expected) in listOf(
                Triple("it-IT", "en-US", "it"),
                Triple("it-CH", "fr-FR", "it"),
                Triple("en-US", "it-IT", "en"),
                Triple("fr-FR", "it-IT", "en"),
                Triple("fr-FR,it-IT", "it-IT", "en"),
                Triple("de-DE", "it-IT", "en"),
                Triple("es-ES", "it-IT", "en"),
            )) {
                manager.applicationLocales = LocaleList.forLanguageTags(app)
                setSystem(system)
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    var retained: MainViewModel? = null
                    scenario.onActivity {
                        assertEquals(expected, it.resources.configuration.locales[0].language)
                        assertEquals(if (expected == "it") "Attiva e riavvia" else "Enable and reboot", it.getString(R.string.enable_reboot))
                        retained = ViewModelProvider(it)[MainViewModel::class.java]
                        assertTrue(retained!!.state.value.paired)
                    }
                    scenario.recreate()
                    scenario.onActivity {
                        assertSame(retained, ViewModelProvider(it)[MainViewModel::class.java])
                        assertEquals(expected, it.resources.configuration.locales[0].language)
                    }
                    assertEquals(identity, store.identity())
                    assertEquals("192.0.2.1", store.pairedHost())
                    assertEquals(pending, store.read())
                }
            }
            // Change the actual system language while the same activity is running.
            manager.applicationLocales = LocaleList.forLanguageTags("it-IT")
            setSystem("fr-FR,it-IT")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                setSystem("it-IT")
                SystemClock.sleep(1_000)
                scenario.onActivity { assertEquals("it", it.resources.configuration.locales[0].language) }
                setSystem("fr-FR,it-IT")
                SystemClock.sleep(1_000)
                scenario.onActivity { assertEquals("en", it.resources.configuration.locales[0].language) }
                assertEquals(identity, store.identity())
                assertEquals(pending, store.read())
            }
        } finally {
            manager.applicationLocales = originalApp
            setSystem(originalSystem.toLanguageTags())
            originals.forEach { (file, bytes) -> if (bytes == null) file.delete() else file.writeBytes(bytes) }
        }
    }
}
