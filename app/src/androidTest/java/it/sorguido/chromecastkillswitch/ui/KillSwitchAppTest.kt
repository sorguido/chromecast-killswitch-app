package it.sorguido.chromecastkillswitch.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fixture UI solo strumentali: non qualificano ADB, VPN o la Chromecast reale. */
class KillSwitchAppTest {
    @get:Rule val compose = createComposeRule()
    private val operations = mutableListOf<Boolean>()
    private val ready = UiState(targetName = "Chromecast di test", paired = true,
        connection = "Collegata", configuration = "Configurazione OFF", canOperate = true)
    private val actions = UiActions({}, {}, { _, _, _ -> }, { _, _ -> }, { operations.add(it) })

    @Test fun everyTapRequiresConfirmationAndCancelDoesNothing() {
        compose.setContent { KillSwitchApp(ready, actions) }
        repeat(2) {
            compose.onNodeWithText("Attiva e riavvia").performScrollTo().performClick()
            compose.onNodeWithText("Attivare e riavviare?").assertIsDisplayed()
            assertEquals(emptyList<Boolean>(), operations)
            compose.onNodeWithText("Annulla").performScrollTo().performClick()
            compose.onNodeWithText("Attivare e riavviare?").assertDoesNotExist()
            assertEquals(emptyList<Boolean>(), operations)
        }
        compose.onNodeWithText("Attiva e riavvia").performScrollTo().performClick()
        compose.onNodeWithText("Conferma").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(true), operations) }
    }

    @Test fun offExplainsAndroidAndProtonBeforeDispatch() {
        compose.setContent { KillSwitchApp(ready, actions) }
        compose.onNodeWithText("Disattiva e riavvia").performScrollTo().performClick()
        compose.onNodeWithText("Disattivare e riavviare?").assertIsDisplayed()
        compose.onNodeWithText("Verranno disattivati always-on e kill switch di Android. L’Auto-connect di Proton resta separato.")
            .performScrollTo().assertIsDisplayed()
        assertEquals(emptyList<Boolean>(), operations)
        compose.onNodeWithText("Conferma").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(false), operations) }
    }

    @Test fun becomingBusyDisablesPendingConfirmationAndBothOperations() {
        val state = mutableStateOf(ready)
        compose.setContent { KillSwitchApp(state.value, actions) }
        compose.onNodeWithText("Attiva e riavvia").performScrollTo().performClick()
        compose.runOnIdle { state.value = ready.copy(busy = true, progress = "Lettura in corso") }
        compose.onNodeWithText("Conferma").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Annulla").performScrollTo().performClick()
        compose.onNodeWithText("Attiva e riavvia").assertIsNotEnabled()
        compose.onNodeWithText("Disattiva e riavvia").assertIsNotEnabled()
        compose.onNodeWithText("Collegamento e associazione").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(emptyList<Boolean>(), operations) }
    }

    @Test fun recoveryGuideIsAvailableWithoutPairingOrNetwork() {
        compose.setContent { KillSwitchApp(UiState(error = "Collegamento non disponibile"), actions) }
        compose.onNodeWithText("Apri la guida di recupero").performScrollTo().performClick()
        compose.onNodeWithText("Se non viene trovata").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Recupero USB dal PC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Le istruzioni qui sopra sono disponibili offline. I collegamenti esterni richiedono rete e accesso al tuo account.")
            .performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<Boolean>(), operations) }
    }

    @Test fun discoveryCanBeRetriedWhileMonitoringIsActive() {
        var searches = 0
        compose.setContent {
            KillSwitchApp(ready.copy(discoveryRunning = true), actions.copy(onDiscover = { searches++ }))
        }
        compose.onNodeWithText("Collegamento e associazione").performScrollTo().performClick()
        compose.onNodeWithText("Ricerca attiva…").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cerca di nuovo").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, searches) }
    }

    @Test fun doubledTextKeepsEssentialActionsReachableByScrolling() = withEmulatorFontScale(2f) {
        compose.setContent { KillSwitchApp(ready, actions) }
        compose.onNodeWithText("Disattiva e riavvia").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Il telefono deve poter raggiungere la Chromecast sulla rete locale e il Debug wireless deve essere attivo.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("La modifica richiede il riavvio della Chromecast e interrompe la riproduzione. Se il Debug wireless è spento, va riattivato dal televisore.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Annulla").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Conferma").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(false), operations) }
    }
}
