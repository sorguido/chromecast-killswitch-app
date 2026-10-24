// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.Choreographer
import it.sorguido.chromecastkillswitch.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Acquisizioni dell'interfaccia realmente renderizzata sull'emulatore con valori sintetici.
 * Non rappresentano pairing, traffico VPN o risultati osservati sulla Chromecast.
 * Fixture e acquisizione esistono soltanto nell'APK dei test strumentali.
 */
class VisualEvidenceTest {
    @get:Rule val compose = createComposeRule()

    private val syntheticReady = UiState(
        targetName = "Chromecast di test",
        connection = UiText(R.string.connection_identified),
        configuration = UiText(R.string.configuration_on),
        detail = UiText(R.string.result_read),
        lastRead = 1_700_000_000_000L,
        paired = true,
        canOperate = true,
    )
    private val noNetworkActions = UiActions({}, {}, { _, _, _ -> }, { _, _ -> }, {})

    private fun render(state: UiState, dark: Boolean = false, locale: Locale = Locale.ITALIAN) {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
            ) {
                LocalizedContent(locale) { KillSwitchApp(state, noNetworkActions) }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // L'idle della semantica Compose non implica che SurfaceFlinger abbia presentato
        // il frame. Attendo due vsync reali e il completamento della composizione video.
        val presentedFrames = CountDownLatch(1)
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback {
                Choreographer.getInstance().postFrameCallback { presentedFrames.countDown() }
            }
        }
        assertTrue("Il display non ha prodotto due frame", presentedFrames.await(5, TimeUnit.SECONDS))
        SystemClock.sleep(1_000)
        val destination = File(instrumentation.targetContext.getExternalFilesDir(null), "evidence")
        check(destination.exists() || destination.mkdirs())
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "Acquisizione schermata non disponibile"
        }
        try {
            // Escludo barre di sistema e bordi: una cattura bianca non è un'evidenza UI.
            var darkest = 255
            var lightest = 0
            for (y in bitmap.height / 5 until bitmap.height * 4 / 5 step 8) {
                for (x in bitmap.width / 5 until bitmap.width * 4 / 5 step 8) {
                    val pixel = bitmap.getPixel(x, y)
                    val luminance = (((pixel shr 16) and 255) + ((pixel shr 8) and 255) + (pixel and 255)) / 3
                    darkest = minOf(darkest, luminance)
                    lightest = maxOf(lightest, luminance)
                }
            }
            assertTrue("Cattura uniforme: il contenuto UI non è stato presentato", lightest - darkest > 40)
            File(destination, "synthetic-$name.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun captureConnectedOnLight() {
        render(syntheticReady)
        compose.onNodeWithText("Configurazione ON").assertIsDisplayed()
        capture("home-on-light")
    }

    @Test fun captureConnectedOnDark() {
        render(syntheticReady, dark = true)
        compose.onNodeWithText("Configurazione ON").assertIsDisplayed()
        capture("home-on-dark")
    }

    @Test fun capturePartialAndConnectionError() {
        render(syntheticReady.copy(
            connection = UiText(R.string.connection_unverified),
            configuration = UiText(R.string.configuration_unverified),
            detail = UiText(R.string.detail_recovery),
            canOperate = false,
            error = UiText(R.string.connection_failed),
        ))
        compose.onNodeWithText("Da verificare").assertIsDisplayed()
        capture("connection-error-stale-partial")
        compose.onNodeWithText("Apri la guida di recupero").performScrollTo().assertIsDisplayed()
        capture("connection-error-recovery")
    }

    @Test fun captureCurrentPartial() {
        render(syntheticReady.copy(
            configuration = UiText(R.string.configuration_partial),
            detail = UiText(R.string.result_read),
        ))
        compose.onNodeWithText("Configurazione parziale").assertIsDisplayed()
        capture("home-partial-light")
    }

    @Test fun captureOffConfirmationAtDoubleFontSize() = withEmulatorFontScale(2f) {
        render(syntheticReady)
        compose.onNodeWithText("Disattiva e riavvia").performScrollTo().performClick()
        compose.onNodeWithText("Disattivare e riavviare?").assertIsDisplayed()
        capture("off-dialog-font200-top")
        compose.onNodeWithText("Conferma").performScrollTo().assertIsDisplayed()
        capture("off-dialog-font200-actions")
    }
    @Test fun captureEnglishHomeLight() {
        render(syntheticReady, locale = Locale.ENGLISH)
        compose.onNodeWithText("ON configuration").assertIsDisplayed()
        capture("home-on-light-en")
    }

    @Test fun captureEnglishHomeDark() {
        render(syntheticReady, dark = true, locale = Locale.ENGLISH)
        compose.onNodeWithText("ON configuration").assertIsDisplayed()
        capture("home-on-dark-en")
    }

    @Test fun captureEnglishErrorAndOfflineGuide() {
        render(syntheticReady.copy(canOperate = false, error = UiText(R.string.failure_readback)), locale = Locale.ENGLISH)
        compose.onNodeWithText("A check is needed").performScrollTo().assertIsDisplayed()
        capture("readback-error-en")
        compose.onNodeWithText("Open the recovery guide").performScrollTo().performClick()
        compose.onNodeWithText("One Chromecast").assertIsDisplayed()
        capture("help-top-en")
        compose.onNodeWithText("USB recovery from a PC").performScrollTo().assertIsDisplayed()
        capture("help-usb-en")
    }

    @Test fun captureEnglishOffConfirmationAtDoubleFontSize() = withEmulatorFontScale(2f) {
        render(syntheticReady, dark = true, locale = Locale.ENGLISH)
        compose.onNodeWithText("Disable and reboot").performScrollTo().performClick()
        compose.onNodeWithText("Disable and reboot?").assertIsDisplayed()
        capture("off-dialog-font200-dark-en-top")
        compose.onNodeWithText("Confirm").performScrollTo().assertIsDisplayed()
        capture("off-dialog-font200-dark-en-actions")
    }

}
