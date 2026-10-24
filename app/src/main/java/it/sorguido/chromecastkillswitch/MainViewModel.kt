package it.sorguido.chromecastkillswitch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.sorguido.chromecastkillswitch.core.*
import it.sorguido.chromecastkillswitch.transport.*
import it.sorguido.chromecastkillswitch.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PrivateStore(application)
    private val transport = AdbTransport(application)
    private val discovery = AdbDiscovery(application)
    private val mutable = MutableStateFlow(UiState(detail = "Associa il telefono alla tua Chromecast per iniziare."))
    val state = mutable.asStateFlow()
    private var foreground = false
    private var worker: Job? = null
    private var discoveryTimer: Job? = null
    private var lastEndpoint: Endpoint? = null
    private var storageHealthy = true

    init {
        try {
            val target = store.identity()
            lastEndpoint = store.endpoint()?.let { Endpoint(it.first, it.second) }
            mutable.update { it.copy(targetName = target?.model, paired = store.pairedHost() != null,
                pending = store.read() != null, detail = if (target == null) it.detail else "Aggiorna per leggere la configurazione attuale.") }
        } catch (_: Exception) {
            storageHealthy = false
            mutable.update { it.copy(error = "Dati locali non leggibili. Nessuna modifica consentita. Ripristina i dati dell’app solo dopo aver verificato la Chromecast dal televisore.") }
        }
        viewModelScope.launch {
            discovery.endpoints.collect { endpoints ->
                mutable.update { it.copy(
                    pairingEndpoints = endpoints.filter { e -> e.kind == EndpointKind.PAIRING }.map(::uiEndpoint),
                    connectEndpoints = endpoints.filter { e -> e.kind == EndpointKind.CONNECT }.map(::uiEndpoint)) }
            }
        }
        viewModelScope.launch { discovery.error.collect { error ->
            if (error != null) mutable.update { it.copy(error = error, discoveryRunning = false) }
        } }
    }

    fun foreground() { foreground = true; discover(); if (state.value.paired) refresh() }
    fun background() { foreground = false; discoveryTimer?.cancel(); discovery.stop(); mutable.update { it.copy(discoveryRunning = false, canOperate = false) } }
    fun discover() {
        if (!foreground || !storageHealthy) return
        discoveryTimer?.cancel()
        discovery.stop()
        discovery.start()
        mutable.update { it.copy(discoveryRunning = true) }
        discoveryTimer = viewModelScope.launch {
            delay(12_000)
            mutable.update { it.copy(discoveryRunning = false) }
        }
    }
    fun pair(host: String, port: Int, code: String) = runWork {
        val endpoint = Endpoint.parse(host, port.toString(), EndpointKind.PAIRING)
        progress("Associazione del telefono in corso…")
        withContext(Dispatchers.IO) { transport.pair(endpoint, code); store.savePairing(endpoint.host) }
        mutable.update { it.copy(paired = true, connection = "Telefono associato", error = null,
            detail = "Ora scegli il servizio di collegamento della stessa Chromecast, oppure inserisci IP e porta dalla schermata Debug wireless del TV.") }
    }
    fun connect(host: String, port: Int) = runWork {
        val endpoint = Endpoint.parse(host, port.toString())
        inspectConnection(listOf(endpoint))
    }
    fun refresh() = runWork {
        val attempted = candidates()
        try { inspectConnection(attempted) }
        catch (firstFailure: Exception) {
            if (!foreground || firstFailure is CancellationException) throw firstFailure
            progress("Ricerca della porta corrente della Chromecast…")
            val resolved = withTimeoutOrNull(12_000) {
                discovery.endpoints.first { endpoints ->
                    withContext(Dispatchers.IO) {
                        endpoints.any { it.kind == EndpointKind.CONNECT &&
                            attempted.none { old -> old.host == it.host && old.port == it.port } &&
                            runCatching { transport.matchesPairedService(it) }.getOrDefault(false) }
                    }
                }
            }
            if (resolved == null) throw firstFailure
            inspectConnection(candidates())
        }
    }

    fun operate(enabled: Boolean) {
        if (!state.value.canOperate || state.value.busy) return
        runWork {
            val expected = withContext(Dispatchers.IO) { store.identity() } ?: error("Target non associato")
            val desired = if (enabled) DesiredState.ON else DesiredState.OFF
            val endpoint = lastEndpoint ?: error("Collegamento assente")
            try {
                withContext(Dispatchers.IO) {
                    transport.connect(endpoint).use { session ->
                        OperationEngine(adapter(session), store, ::onStage).execute(desired, expected)
                    }
                }
            } catch (e: OperationException) {
                if (e.failure != Failure.REBOOT_UNCERTAIN) throw e
                // La richiesta potrebbe essere arrivata: segue solo riconciliazione, mai replay.
            }
            mutable.update { it.copy(pending = true, connection = "In attesa del riavvio", configuration = "Da verificare",
                detail = "Modifica salvata. Attendo un nuovo avvio e rileggo la configurazione.") }
            repeat(12) {
                if (!foreground) return@runWork
                delay(5_000)
                try {
                    inspectConnection(candidates(), announceFailure = false)
                    if (!state.value.pending) return@runWork
                } catch (_: Exception) { /* Riprova solo letture, con limite finito. */ }
            }
            mutable.update { it.copy(connection = "Verifica da completare", configuration = "Da verificare", canOperate = false,
                detail = "Il riavvio non è ancora verificato. Se necessario riattiva il Debug wireless dal TV, poi premi Aggiorna. Nessun comando verrà ripetuto automaticamente.") }
        }
    }

    private suspend fun inspectConnection(endpoints: List<Endpoint>, announceFailure: Boolean = true) {
        if (endpoints.isEmpty()) throw UserFacingException("Nessuna Chromecast trovata. Apri il Debug wireless sul TV, cerca di nuovo oppure inserisci IP e porta correnti.")
        val expected = withContext(Dispatchers.IO) { store.identity() }
        withContext(Dispatchers.IO) { store.pairedHost() } ?: throw IllegalStateException("Associazione necessaria")
        var lastFailure: Exception? = null
        for (endpoint in endpoints.take(8)) {
            try {
                val result = withContext(Dispatchers.IO) {
                    transport.connect(endpoint).use { session ->
                        val engine = OperationEngine(adapter(session), store, ::onStage)
                        if (expected == null) {
                            val snapshot = engine.inspect()
                            store.saveIdentity(snapshot.identity)
                            Reconciliation(snapshot, ReconciliationStatus.NO_PENDING_OPERATION)
                        } else engine.reconcile(expected)
                    }.also { store.saveEndpoint(endpoint.host, endpoint.port) }
                }
                lastEndpoint = endpoint
                val pending = withContext(Dispatchers.IO) { store.read() != null }
                mutable.update { it.copy(targetName = result.snapshot.identity.model, paired = true,
                    connection = "Collegata e identificata", configuration = configurationLabel(result.snapshot.configuration),
                    lastRead = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()), pending = pending,
                    canOperate = foreground && result.snapshot.configuration != Configuration.CONFLICT && result.snapshot.bootCompleted,
                    error = null, detail = resultText(result), progress = null) }
                return
            } catch (e: Exception) { lastFailure = e }
        }
        if (announceFailure) throw lastFailure ?: IllegalStateException("Collegamento non disponibile")
        throw lastFailure ?: IllegalStateException("Collegamento non disponibile")
    }

    private suspend fun candidates(): List<Endpoint> = withContext(Dispatchers.IO) {
        val found = discovery.endpoints.value.filter { it.kind == EndpointKind.CONNECT }
        val matching = found.filter { runCatching { transport.matchesPairedService(it) }.getOrDefault(false) }
        (matching + listOfNotNull(lastEndpoint) + found).distinctBy { it.host to it.port }
    }

    private fun runWork(block: suspend () -> Unit) {
        if (!storageHealthy || worker?.isActive == true) return
        mutable.update { it.copy(busy = true, canOperate = false, error = null, configuration = "Da verificare", progress = null) }
        worker = viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = when (e) {
                    is OperationException -> e.message ?: "Operazione non verificata."
                    is TransportException -> e.message ?: "Collegamento non verificato."
                    is UserFacingException -> e.message ?: "Stato da verificare."
                    is IllegalArgumentException -> "Controlla indirizzo, porta e codice: usa i valori correnti mostrati sul TV."
                    is SecurityException -> "Accesso alla rete negato. Controlla i permessi dell’app nelle impostazioni del telefono."
                    else -> "Collegamento non riuscito o autorizzazione scaduta. Controlla rete locale e Debug wireless sul TV; se necessario associa di nuovo il telefono."
                }
                mutable.update { it.copy(connection = "Non verificata", configuration = "Da verificare", canOperate = false, error = message,
                    detail = "L’app non può riaccendere il Debug wireless da remoto. Apri la guida, riprova oppure usa l’endpoint corrente.") }
            } finally {
                val pending = runCatching { withContext(Dispatchers.IO) { store.read() != null } }.getOrDefault(true)
                mutable.update { it.copy(busy = false, pending = pending, progress = null) }
            }
        }
    }

    private fun onStage(stage: OperationStage) {
        progress(when (stage) {
            OperationStage.PREFLIGHT -> "Controllo Chromecast, profilo e configurazione…"
            OperationStage.WRITING -> "Salvataggio delle impostazioni Android…"
            OperationStage.READBACK -> "Verifica delle impostazioni salvate…"
            OperationStage.SAVED -> "Configurazione salvata e riletta."
            OperationStage.REBOOT_REQUESTED -> "Richiesta di riavvio…"
            OperationStage.WAITING_FOR_REBOOT -> "Attesa del nuovo avvio…"
            OperationStage.VERIFIED -> "Configurazione verificata dopo il riavvio."
        })
    }
    private fun progress(text: String) { mutable.update { it.copy(progress = text) } }
    private fun uiEndpoint(e: Endpoint) = UiEndpoint("${e.kind}:${e.address}", e.serviceName.ifBlank { "Chromecast sulla rete" }, e.host, e.port)
    private fun adapter(session: AdbSession) = object : CommandSession {
        override fun shell(command: String) = session.shell(command)
        override fun reboot() = session.reboot()
    }
    private fun configurationLabel(c: Configuration) = when(c) {
        Configuration.ON -> "Configurazione ON"
        Configuration.OFF -> "Configurazione OFF"
        Configuration.PARTIAL -> "Configurazione parziale"
        Configuration.CONFLICT -> "Configurazione in conflitto"
    }
    private fun resultText(result: Reconciliation): String = when (result.status) {
        ReconciliationStatus.VERIFIED -> "Configurazione verificata dopo un nuovo avvio. Lo stato del tunnel VPN resta separato."
        ReconciliationStatus.NO_PENDING_OPERATION -> if (result.snapshot.configuration == Configuration.CONFLICT)
            "È presente un altro provider o una configurazione con eccezioni. Nessuna modifica consentita."
            else "Impostazioni Android lette ora. Questo stato non certifica che il tunnel VPN sia connesso."
        ReconciliationStatus.WAITING_FOR_REBOOT -> "Operazione in sospeso: un nuovo avvio non è ancora verificato. Attendi e aggiorna; nessun comando viene ripetuto."
        ReconciliationStatus.BOOT_IN_PROGRESS -> "La Chromecast sta completando l’avvio. Attendi e aggiorna."
        ReconciliationStatus.STATE_MISMATCH -> "Il nuovo avvio è stato rilevato, ma la configurazione non corrisponde alla richiesta. Scegli esplicitamente come procedere."
        else -> "Operazione interrotta prima della conferma del riavvio. Controlla lo stato e scegli esplicitamente se riprovare."
    }
    override fun onCleared() { discovery.stop(); super.onCleared() }
}

private class UserFacingException(message: String) : Exception(message)
