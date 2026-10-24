package it.sorguido.chromecastkillswitch.ui

import it.sorguido.chromecastkillswitch.transport.Endpoint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Valori presentati solo quando il controller li ha rilevati; nessun dato simulato nella UI. */
data class UiState(
    val targetName: String? = null,
    val connection: String = "Non collegata",
    val configuration: String = "Da verificare",
    val detail: String = "Collega la Chromecast per leggere le impostazioni di Android.",
    val lastRead: String? = null,
    val progress: String? = null,
    val pending: Boolean = false,
    val busy: Boolean = false,
    val canOperate: Boolean = false,
    val paired: Boolean = false,
    val discoveryRunning: Boolean = false,
    val pairingEndpoints: List<UiEndpoint> = emptyList(),
    val connectEndpoints: List<UiEndpoint> = emptyList(),
    val error: String? = null,
)

data class UiEndpoint(val id: String, val label: String, val host: String, val port: Int)

data class UiActions(
    val onRefresh: () -> Unit,
    val onDiscover: () -> Unit,
    val onPair: (host: String, port: Int, code: String) -> Unit,
    val onConnect: (host: String, port: Int) -> Unit,
    val onOperate: (enabled: Boolean) -> Unit,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B60), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEDE5), onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF435F58), onSecondary = Color.White,
    background = Color(0xFFF5F7F3), onBackground = Color(0xFF182520),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF182520),
    surfaceVariant = Color(0xFFE6EDE7), onSurfaceVariant = Color(0xFF43534A),
    outline = Color(0xFF68796E), error = Color(0xFF9E2535),
    errorContainer = Color(0xFFFFDADD), onErrorContainer = Color(0xFF690014),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D4C3), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF184D43), onPrimaryContainer = Color(0xFFCCEDE5),
    secondary = Color(0xFFB3CDC2), onSecondary = Color(0xFF20392F),
    background = Color(0xFF101A16), onBackground = Color(0xFFE5EEE7),
    surface = Color(0xFF19261F), onSurface = Color(0xFFE5EEE7),
    surfaceVariant = Color(0xFF2B3C32), onSurfaceVariant = Color(0xFFBDCEC2),
    outline = Color(0xFF8B9F91), error = Color(0xFFFFB2BB),
    errorContainer = Color(0xFF6D1C2A), onErrorContainer = Color(0xFFFFDADD),
)

@Composable
fun KillSwitchApp(state: UiState, actions: UiActions) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        var section by remember { mutableStateOf<String?>(null) }
        var requestedOperation by remember { mutableStateOf<Boolean?>(null) }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("IL TUO TELECOMANDO", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Chromecast\nKill Switch", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                        Text("Always-on e kill switch di Android, insieme.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(state.targetName ?: "La tua Chromecast", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(state.connection, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Text("CONFIGURAZIONE ANDROID", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.configuration, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            Text(state.detail, style = MaterialTheme.typography.bodyMedium)
                            state.lastRead?.let {
                                Text("Ultima lettura: $it", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (state.busy || state.progress != null || state.pending) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(state.progress ?: "Operazione da verificare", fontWeight = FontWeight.Medium,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                if (state.pending) Text(
                                    "Il risultato non è ancora confermato. Rileggi lo stato dopo il riavvio: l’app non ripete automaticamente modifiche o riavvii.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    state.error?.let { error ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Serve un controllo", style = MaterialTheme.typography.titleMedium)
                                Text(error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                Text("Se il Debug wireless è spento, riattivalo dal televisore. L’app non può accenderlo da sola.",
                                    style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { section = "help" }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text("Apri la guida di recupero")
                                }
                            }
                        }
                    }
                    if (!state.paired) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Inizia dal televisore", style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() })
                            Text("Associa questo telefono con il codice mostrato dalla Chromecast. Bastano pochi passaggi, senza un PC.")
                            PrimaryAction("Associa la Chromecast", !state.busy) {
                                section = "pair"; actions.onDiscover()
                            }
                        }
                    } else if (state.targetName == null) {
                        Text("Associazione eseguita. Ora scegli la porta di collegamento per riconoscere la Chromecast e leggere lo stato.")
                        PrimaryAction("Collega la Chromecast", !state.busy) {
                            section = "connect"; actions.onDiscover()
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryAction("Attiva e riavvia", state.canOperate && !state.busy) {
                            requestedOperation = true
                        }
                        OutlinedButton(
                            onClick = { requestedOperation = false }, enabled = state.canOperate && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        ) { Text("Disattiva e riavvia", style = MaterialTheme.typography.titleMedium) }
                        Text("La modifica riavvia la Chromecast e interrompe la riproduzione. Lo stato delle impostazioni non indica se il tunnel VPN è connesso.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Column {
                        TextButton(onClick = actions.onRefresh, enabled = state.paired && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Rileggi lo stato") }
                        TextButton(onClick = { section = if (state.paired) "connect" else "pair"; actions.onDiscover() },
                            enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Collegamento e associazione")
                        }
                        TextButton(onClick = { section = "help" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Guida e recupero")
                        }
                    }
                }
            }
        }
        requestedOperation?.let { enabled ->
            SheetDialog(if (enabled) "Attivare e riavviare?" else "Disattivare e riavviare?", { requestedOperation = null }) {
                Text("Il telefono deve poter raggiungere la Chromecast sulla rete locale e il Debug wireless deve essere attivo.")
                Text("La modifica richiede il riavvio della Chromecast e interrompe la riproduzione. Se il Debug wireless è spento, va riattivato dal televisore.")
                Text(if (enabled) "Verranno attivati always-on e kill switch di Android per Proton VPN."
                    else "Verranno disattivati always-on e kill switch di Android. L’Auto-connect di Proton resta separato.")
                OutlinedButton(onClick = { requestedOperation = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Annulla")
                }
                PrimaryAction("Conferma", !state.busy && state.canOperate) {
                    requestedOperation = null
                    actions.onOperate(enabled)
                }
            }
        }
        when (section) {
            "pair" -> ConnectionDialog(state, actions, pairing = true, close = { section = null },
                changeMode = { section = "connect" })
            "connect" -> ConnectionDialog(state, actions, pairing = false, close = { section = null },
                changeMode = { section = "pair" })
            "help" -> HelpDialog { section = null }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SheetDialog(title: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(20.dp).widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() })
                content()
            }
        }
    }
}

@Composable
private fun ConnectionDialog(state: UiState, actions: UiActions, pairing: Boolean, close: () -> Unit, changeMode: () -> Unit) {
    var host by remember(pairing) { mutableStateOf("") }
    var port by remember(pairing) { mutableStateOf("") }
    // Il codice non entra in SavedState, preferenze o diagnostica.
    var code by remember(pairing) { mutableStateOf("") }
    var attempted by remember(pairing) { mutableStateOf(false) }
    val normalizedHost = host.trim().removePrefix("[").removeSuffix("]")
    val portNumber = port.toIntOrNull()
    val validHost = runCatching { Endpoint(normalizedHost, 1) }.isSuccess
    val validPort = portNumber != null && portNumber in 1..65535
    val validCode = !pairing || code.matches(Regex("[0-9]{6}"))
    SheetDialog(if (pairing) "Associa il telefono" else "Collega la Chromecast", close) {
        TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text("Torna alla schermata principale") }
        if (pairing) {
            Text("1. Sul televisore apri Impostazioni → Sistema → Opzioni sviluppatore → Debug wireless.")
            Text("2. Scegli «Associa dispositivo con codice di accoppiamento» e lascia aperta questa schermata sul TV.")
            Text("3. Seleziona qui la Chromecast oppure inserisci l’indirizzo e la porta di associazione mostrati sul TV, quindi il codice.")
            Text("L’autorizzazione del PC è distinta. Sul telefono non serve attivare il Debug wireless.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Usa indirizzo e porta della schermata principale «Debug wireless» sul televisore. La porta di collegamento è diversa da quella di associazione e può cambiare dopo un riavvio.")
            Text("La Chromecast deve essere già associata a questo telefono. Un indirizzo manuale non sostituisce l’autorizzazione.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.progress?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Sulla rete locale", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val endpoints = if (pairing) state.pairingEndpoints else state.connectEndpoints
        if (endpoints.isEmpty()) Text(if (state.discoveryRunning) "Ricerca attiva…"
            else "Nessun dispositivo trovato. Controlla il Debug wireless sul TV e riprova, oppure inserisci l’indirizzo qui sotto. La guida di recupero è disponibile nella schermata principale.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        endpoints.forEach { endpoint ->
            OutlinedButton(onClick = { host = endpoint.host; port = endpoint.port.toString(); attempted = false },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(16.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(endpoint.label, style = MaterialTheme.typography.labelLarge)
                    Text("${endpoint.host} · ${endpoint.port}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = actions.onDiscover, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cerca di nuovo") }
        HorizontalDivider()
        Text(if (pairing) "Indirizzo di associazione" else "Indirizzo di collegamento", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = host, onValueChange = { host = it }, enabled = !state.busy,
            label = { Text("Indirizzo IP della Chromecast") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), isError = attempted && !validHost,
            supportingText = if (attempted && !validHost) ({ Text("Inserisci un indirizzo IPv4 o IPv6 valido.") }) else null)
        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, enabled = !state.busy,
            label = { Text(if (pairing) "Porta di associazione" else "Porta di collegamento") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = attempted && !validPort,
            supportingText = if (attempted && !validPort) ({ Text("Inserisci la porta mostrata sul TV, da 1 a 65535.") }) else null)
        if (pairing) OutlinedTextField(value = code, onValueChange = { code = it.filter { c -> c in '0'..'9' }.take(6) },
            enabled = !state.busy, label = { Text("Codice di 6 cifre") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), isError = attempted && !validCode,
            supportingText = { Text(if (attempted && !validCode) "Inserisci le 6 cifre mostrate sul TV." else "Il codice viene usato solo per questa associazione.") })
        PrimaryAction(if (pairing) "Associa questo telefono" else "Collega e leggi lo stato", !state.busy && (pairing || state.paired)) {
            attempted = true
            if (validHost && validPort && validCode) {
                if (pairing) {
                    val submittedCode = code
                    code = ""
                    actions.onPair(normalizedHost, portNumber!!, submittedCode)
                } else {
                    actions.onConnect(normalizedHost, portNumber!!)
                    close()
                }
            }
        }
        if (pairing && state.paired) {
            Text("Questo telefono ha un’associazione salvata. Passa al collegamento usando la relativa porta mostrata sul TV.")
            PrimaryAction("Passa al collegamento", !state.busy, changeMode)
        } else if (!pairing) {
            TextButton(onClick = changeMode, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Associa di nuovo il telefono")
            }
        }
    }
}

@Composable
private fun HelpDialog(close: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    SheetDialog("Guida e recupero", close) {
        TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text("Torna alla schermata principale") }
        HelpSection("Una sola Chromecast", "Questa app è pensata per Chromecast con Google TV HD, Android 14, utente principale e Proton VPN già configurata. Il telefono usa una propria associazione. La Chromecast viene riconosciuta prima di ogni modifica.")
        HelpSection("Se non viene trovata", "Collega telefono e Chromecast alla stessa rete locale, evitando reti ospiti o isolamento tra dispositivi. Sul TV apri Impostazioni → Sistema → Opzioni sviluppatore → Debug wireless e verifica che sia acceso. L’app non può riaccenderlo da sola. Se il telefono nega l’accesso alla rete locale, controlla i permessi dell’app nelle impostazioni del telefono.")
        HelpSection("Associazione e collegamento", "Sul TV scegli l’associazione tramite codice. Nell’app apri «Collegamento e associazione», seleziona la Chromecast o inserisci IP, porta di associazione e codice. Dopo l’associazione, torna alla schermata principale del Debug wireless sul TV: usa la porta di collegamento, diversa da quella del codice. Puoi inserirla manualmente se la ricerca non riesce.")
        HelpSection("Attiva e riavvia", "Dopo la conferma l’app controlla il dispositivo, salva always-on e kill switch di Android per Proton, rilegge le impostazioni e solo dopo richiede il riavvio. Una configurazione ON non dimostra che il tunnel VPN sia connesso né certifica l’assenza di perdite.")
        HelpSection("Disattiva e riavvia", "Rimuove entrambe le funzioni di Android, anche se era rimasta attiva soltanto la VPN always-on. L’Auto-connect di Proton è un’impostazione distinta: può ancora connettere la VPN e si gestisce nell’app Proton sulla Chromecast.")
        HelpSection("Dopo il riavvio", "La perdita del collegamento è prevista. Attendi l’avvio e usa «Rileggi lo stato». Se il Debug wireless è rimasto spento, riattivalo dal TV e cerca di nuovo, oppure inserisci il nuovo indirizzo di collegamento. Senza una nuova verifica il risultato resta non confermato. Non occorre lasciare l’app sempre aperta.")
        HelpSection("Configurazione parziale o errore", "Non presumere che la modifica sia riuscita. Ripristina il collegamento e rileggi lo stato. Dopo aver verificato la Chromecast puoi scegliere esplicitamente l’operazione desiderata. L’app non ripete da sola scritture o riavvii e non disattiva una protezione per compensare un errore.")
        HelpSection("Associazione revocata o dati non leggibili", "Se il TV ha revocato il telefono, associa di nuovo usando un nuovo codice: la stessa Chromecast resta selezionata. Se il dispositivo è stato ripristinato o i dati locali sono danneggiati, verifica prima la configurazione sul TV e completa l’eventuale recupero. Solo dopo puoi cancellare i dati di questa app dalle impostazioni Android e rifare l’associazione: cancellare i dati elimina anche il promemoria delle operazioni non concluse.")
        HelpSection("Recupero USB dal PC", "Se il collegamento wireless non torna, è disponibile il recupero USB già verificato dall’utente: cavo dati, alimentazione adeguata, Debug USB attivo sulla Chromecast e PC autorizzato. La procedura canonica elimina le tre impostazioni, controlla la rimozione e solo dopo riavvia. Non serve normalmente un reset di fabbrica. Consulta il contratto nel sorgente o la guida personale prima di procedere.")
        TextButton(onClick = {
            try { uriHandler.openUri("https://github.com/sorguido/chromecast-killswitch-app/blob/main/docs/COMMAND_CONTRACT.md") }
            catch (_: Exception) { linkError = true }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Procedura USB nel repository") }
        TextButton(onClick = {
            try { uriHandler.openUri("https://github.com/sorguido/chromecast-killswitch-app/blob/main/docs/COMMAND_CONTRACT.md") }
            catch (_: Exception) { linkError = true }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Guida personale Chromecast") }
        Text("Le istruzioni qui sopra sono disponibili offline. I collegamenti esterni richiedono rete e accesso al tuo account.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (linkError) Text("Nessuna app disponibile per aprire il collegamento. La guida offline resta consultabile.", color = MaterialTheme.colorScheme.error)
        PrimaryAction("Ho capito", onClick = close)
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
