// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.sorguido.chromecastkillswitch.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Synthetic rendering checks: no remote device, pairing or VPN claim. */
class LocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun retainedMessagesAndOpenConfirmationFollowLocaleWithoutDispatch() {
        val locale = mutableStateOf(Locale.ITALIAN)
        val operations = mutableListOf<Boolean>()
        val state = UiState(paired = true, targetName = "Synthetic Chromecast", canOperate = true,
            connection = UiText(R.string.connection_identified),
            configuration = UiText(R.string.configuration_on),
            detail = UiText(R.string.result_verified),
            error = UiText(R.string.failure_readback))
        val actions = UiActions({}, {}, { _, _, _ -> }, { _, _ -> }, { operations.add(it) })
        compose.setContent { LocalizedContent(locale.value) { KillSwitchApp(state, actions) } }
        compose.onNodeWithText("Configurazione ON").assertIsDisplayed()
        compose.onNodeWithText("Attiva e riavvia").performScrollTo().performClick()
        compose.onNodeWithText("Attivare e riavviare?").assertIsDisplayed()
        compose.runOnIdle { locale.value = Locale.ENGLISH }
        compose.onNodeWithText("Enable and reboot?").assertIsDisplayed()
        compose.onNodeWithText("Android always-on and kill switch will be enabled for Proton VPN.")
            .performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<Boolean>(), operations) }
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Readback did not confirm the change. No reboot was sent. Restore the connection, read the state again and explicitly choose how to proceed.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open the recovery guide").performScrollTo().performClick()
        compose.onNodeWithText("USB recovery from a PC").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<Boolean>(), operations) }
    }

    @Test fun englishPairingAndValidationAreLocalized() {
        val actions = UiActions({}, {}, { _, _, _ -> }, { _, _ -> }, {})
        compose.setContent { LocalizedContent(Locale.ENGLISH) { KillSwitchApp(UiState(), actions) } }
        compose.onNodeWithText("Pair the Chromecast").performScrollTo().performClick()
        compose.onNodeWithText("Pair the phone").assertIsDisplayed()
        compose.onNodeWithText("Pair this phone").performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid IPv4 or IPv6 address.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Enter the 6 digits shown on the TV.").performScrollTo().assertIsDisplayed()
    }
}
