// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PrivateStore(application)
    private val transport = AdbTransport(application)
    private val discovery = AdbDiscovery(application)
    private val mutable = MutableStateFlow(UiState(detail = UiText(R.string.detail_start)))
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
                pending = store.read() != null, detail = if (target == null) it.detail else UiText(R.string.detail_refresh)) }
        } catch (_: Exception) {
            storageHealthy = false
            mutable.update { it.copy(error = UiText(R.string.storage_unreadable)) }
        }
        viewModelScope.launch {
            discovery.endpoints.collect { endpoints ->
                mutable.update { it.copy(
                    pairingEndpoints = endpoints.filter { e -> e.kind == EndpointKind.PAIRING }.map(::uiEndpoint),
                    connectEndpoints = endpoints.filter { e -> e.kind == EndpointKind.CONNECT }.map(::uiEndpoint)) }
            }
        }
        viewModelScope.launch { discovery.error.collect { error ->
            if (error != null) mutable.update { it.copy(error = error.uiText(), discoveryRunning = false) }
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
        progress(UiText(R.string.progress_pairing))
        withContext(Dispatchers.IO) { transport.pair(endpoint, code); store.savePairing(endpoint.host) }
        mutable.update { it.copy(paired = true, connection = UiText(R.string.connection_paired), error = null,
            detail = UiText(R.string.detail_paired)) }
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
            progress(UiText(R.string.progress_find_port))
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
            mutable.update { it.copy(pending = true, connection = UiText(R.string.connection_reboot_wait), configuration = UiText(R.string.configuration_unverified),
                detail = UiText(R.string.detail_saved)) }
            repeat(12) {
                if (!foreground) return@runWork
                delay(5_000)
                try {
                    inspectConnection(candidates(), announceFailure = false)
                    if (!state.value.pending) return@runWork
                } catch (_: Exception) { /* Riprova solo letture, con limite finito. */ }
            }
            mutable.update { it.copy(connection = UiText(R.string.connection_check_pending), configuration = UiText(R.string.configuration_unverified), canOperate = false,
                detail = UiText(R.string.detail_reboot_unverified)) }
        }
    }

    private suspend fun inspectConnection(endpoints: List<Endpoint>, announceFailure: Boolean = true) {
        if (endpoints.isEmpty()) throw UserFacingException(UiText(R.string.no_chromecast))
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
                    connection = UiText(R.string.connection_identified), configuration = configurationLabel(result.snapshot.configuration),
                    lastRead = System.currentTimeMillis(), pending = pending,
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
        mutable.update { it.copy(busy = true, canOperate = false, error = null, configuration = UiText(R.string.configuration_unverified), progress = null) }
        worker = viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = when (e) {
                    is OperationException -> e.failure.uiText()
                    is TransportException -> e.reason.uiText()
                    is UserFacingException -> e.text
                    is IllegalArgumentException -> UiText(R.string.invalid_endpoint)
                    is SecurityException -> UiText(R.string.network_denied)
                    else -> UiText(R.string.connection_failed)
                }
                mutable.update { it.copy(connection = UiText(R.string.connection_unverified), configuration = UiText(R.string.configuration_unverified), canOperate = false, error = message,
                    detail = UiText(R.string.detail_recovery)) }
            } finally {
                val pending = runCatching { withContext(Dispatchers.IO) { store.read() != null } }.getOrDefault(true)
                mutable.update { it.copy(busy = false, pending = pending, progress = null) }
            }
        }
    }

    private fun onStage(stage: OperationStage) {
        progress(when (stage) {
            OperationStage.PREFLIGHT -> UiText(R.string.progress_preflight)
            OperationStage.WRITING -> UiText(R.string.progress_writing)
            OperationStage.READBACK -> UiText(R.string.progress_readback)
            OperationStage.SAVED -> UiText(R.string.progress_saved)
            OperationStage.REBOOT_REQUESTED -> UiText(R.string.progress_reboot_requested)
            OperationStage.WAITING_FOR_REBOOT -> UiText(R.string.progress_waiting_reboot)
            OperationStage.VERIFIED -> UiText(R.string.progress_verified)
        })
    }
    private fun progress(text: UiText) { mutable.update { it.copy(progress = text) } }
    private fun uiEndpoint(e: Endpoint) = UiEndpoint("${e.kind}:${e.address}", e.serviceName, e.host, e.port)
    private fun adapter(session: AdbSession) = object : CommandSession {
        override fun shell(command: String) = session.shell(command)
        override fun reboot() = session.reboot()
    }
    private fun configurationLabel(c: Configuration) = when(c) {
        Configuration.ON -> UiText(R.string.configuration_on)
        Configuration.OFF -> UiText(R.string.configuration_off)
        Configuration.PARTIAL -> UiText(R.string.configuration_partial)
        Configuration.CONFLICT -> UiText(R.string.configuration_conflict)
    }
    private fun resultText(result: Reconciliation): UiText = when (result.status) {
        ReconciliationStatus.VERIFIED -> UiText(R.string.result_verified)
        ReconciliationStatus.NO_PENDING_OPERATION -> if (result.snapshot.configuration == Configuration.CONFLICT)
            UiText(R.string.result_conflict)
            else UiText(R.string.result_read)
        ReconciliationStatus.WAITING_FOR_REBOOT -> UiText(R.string.result_waiting)
        ReconciliationStatus.BOOT_IN_PROGRESS -> UiText(R.string.result_booting)
        ReconciliationStatus.STATE_MISMATCH -> UiText(R.string.result_mismatch)
        else -> UiText(R.string.result_interrupted)
    }
    override fun onCleared() { discovery.stop(); super.onCleared() }
}

private class UserFacingException(val text: UiText) : Exception()
