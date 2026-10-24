package it.sorguido.chromecastkillswitch.core

import java.util.concurrent.atomic.AtomicBoolean

/** Il trasporto restituisce solo output completo di un comando con exit code zero. */
interface CommandSession {
    fun shell(command: String): String
    fun reboot()
}

/** Tutte le scritture devono essere durevoli prima del ritorno, oppure sollevare un errore. */
interface PendingStore {
    fun read(): PendingOperation?
    fun write(operation: PendingOperation)
    fun clear()
}

data class Identity(
    val serial: String,
    val model: String,
    val device: String,
    val sdk: Int,
    val release: String,
) {
    override fun toString(): String = "Identity(model=$model, device=$device, sdk=$sdk)"
}

enum class Configuration { ON, OFF, PARTIAL, CONFLICT }
enum class DesiredState { ON, OFF }
enum class OperationStage { PREFLIGHT, WRITING, READBACK, SAVED, REBOOT_REQUESTED, WAITING_FOR_REBOOT, VERIFIED }

data class VpnSettings(val app: String, val lockdown: String, val whitelist: String) {
    val configuration: Configuration
        get() = when {
            app !in setOf("null", PROTON_PACKAGE) -> Configuration.CONFLICT
            lockdown !in setOf("null", "0", "1") -> Configuration.CONFLICT
            whitelist !in setOf("null", "") -> Configuration.CONFLICT
            app == PROTON_PACKAGE && lockdown == "1" -> Configuration.ON
            app == "null" && lockdown in setOf("null", "0") -> Configuration.OFF
            else -> Configuration.PARTIAL
        }

    fun matches(desired: DesiredState): Boolean = when (desired) {
        DesiredState.ON -> configuration == Configuration.ON
        DesiredState.OFF -> configuration == Configuration.OFF
    }

    fun matchesBeforeReboot(desired: DesiredState): Boolean = when (desired) {
        DesiredState.ON -> configuration == Configuration.ON
        DesiredState.OFF -> app == "null" && lockdown == "null" && whitelist == "null"
    }
}

data class Snapshot(
    val identity: Identity,
    val settings: VpnSettings,
    val bootId: String?,
    val bootCompleted: Boolean,
) {
    val configuration: Configuration get() = settings.configuration
}

data class PendingOperation(
    val targetSerial: String,
    val desired: DesiredState,
    val beforeBootId: String,
    val phase: OperationStage,
) {
    override fun toString(): String = "PendingOperation(desired=$desired, phase=$phase)"
}

enum class ReconciliationStatus { NO_PENDING_OPERATION, REQUIRES_EXPLICIT_RETRY, WAITING_FOR_REBOOT, BOOT_IN_PROGRESS, STATE_MISMATCH, VERIFIED }
data class Reconciliation(val snapshot: Snapshot, val status: ReconciliationStatus)

enum class Failure {
    BUSY, READ_FAILED, IDENTITY_UNAVAILABLE, WRONG_TARGET, UNSUPPORTED_TARGET,
    WRONG_USER, PROTON_MISSING, CONFIGURATION_CONFLICT, BOOT_UNVERIFIABLE,
    WRITE_FAILED, READBACK_FAILED, STORAGE_FAILED, REBOOT_UNCERTAIN,
}

class OperationException(val failure: Failure, message: String) : Exception(message)

const val PROTON_PACKAGE = "ch.protonvpn.android"

/**
 * Nessun metodo avvia discovery, pairing o retry. La connessione è già autenticata.
 * execute viene chiamato soltanto dopo una nuova conferma esplicita nella UI.
 * reconcile è sempre di sola lettura sul dispositivo, anche dopo una terminazione del processo.
 */
class OperationEngine(
    private val session: CommandSession,
    private val store: PendingStore,
    private val onProgress: (OperationStage) -> Unit = {},
) {
    private val busy = AtomicBoolean(false)

    fun inspect(expected: Identity? = null): Snapshot = exclusive { inspectInternal(expected) }

    fun execute(desired: DesiredState, expected: Identity): PendingOperation = exclusive {
        onProgress(OperationStage.PREFLIGHT)
        val before = inspectInternal(expected)
        if (before.configuration == Configuration.CONFLICT) fail(
            Failure.CONFIGURATION_CONFLICT,
            "Configurazione estranea o eccezioni VPN presenti. Nessuna modifica eseguita.",
        )
        val beforeBootId = before.bootId ?: fail(
            Failure.BOOT_UNVERIFIABLE,
            "Impossibile identificare l'avvio della Chromecast. Nessuna modifica eseguita: riprova la lettura.",
        )
        if (!before.bootCompleted) fail(
            Failure.BOOT_UNVERIFIABLE,
            "La Chromecast sta ancora completando l'avvio. Attendi e riprova.",
        )
        var pending = PendingOperation(expected.serial, desired, beforeBootId, OperationStage.WRITING)
        persist(pending)
        onProgress(OperationStage.WRITING)
        for (command in commands(desired)) {
            try {
                session.shell(command)
            } catch (_: Exception) {
                fail(Failure.WRITE_FAILED, "Modifica interrotta: la configurazione potrebbe essere parziale. Nessun riavvio inviato. Aggiorna lo stato prima di riprovare.")
            }
        }
        onProgress(OperationStage.READBACK)
        val after = try {
            inspectInternal(expected)
        } catch (_: Exception) {
            fail(Failure.READBACK_FAILED, "Modifica non verificata. Nessun riavvio inviato. Ripristina il collegamento e aggiorna lo stato.")
        }
        if (after.bootId != beforeBootId || !after.bootCompleted || !after.settings.matchesBeforeReboot(desired)) {
            fail(Failure.READBACK_FAILED, "La rilettura non conferma la modifica. Nessun riavvio inviato. Aggiorna lo stato e scegli esplicitamente come procedere.")
        }
        pending = pending.copy(phase = OperationStage.SAVED)
        persist(pending)
        onProgress(OperationStage.SAVED)
        pending = pending.copy(phase = OperationStage.REBOOT_REQUESTED)
        persist(pending) // Il crash dopo questo punto deve lasciare il riavvio incerto, mai da ripetere.
        onProgress(OperationStage.REBOOT_REQUESTED)
        try {
            session.reboot()
        } catch (_: Exception) {
            fail(Failure.REBOOT_UNCERTAIN, "Configurazione salvata; riavvio non confermato. Attendi e aggiorna lo stato. Nessun riavvio sarà ripetuto automaticamente.")
        }
        pending = pending.copy(phase = OperationStage.WAITING_FOR_REBOOT)
        persist(pending)
        onProgress(OperationStage.WAITING_FOR_REBOOT)
        pending
    }

    fun reconcile(expected: Identity): Reconciliation = exclusive {
        val pending = try { store.read() } catch (_: Exception) {
            fail(Failure.STORAGE_FAILED, "Impossibile leggere l'operazione salvata. Nessuna modifica eseguita.")
        }
        if (pending != null && pending.targetSerial != expected.serial) {
            fail(Failure.WRONG_TARGET, "L'operazione in sospeso appartiene a un altro dispositivo. Nessuna modifica eseguita.")
        }
        val snapshot = inspectInternal(expected)
        val status = when {
            pending == null -> ReconciliationStatus.NO_PENDING_OPERATION
            pending.phase !in setOf(OperationStage.REBOOT_REQUESTED, OperationStage.WAITING_FOR_REBOOT) -> ReconciliationStatus.REQUIRES_EXPLICIT_RETRY
            snapshot.bootId == null || snapshot.bootId == pending.beforeBootId -> ReconciliationStatus.WAITING_FOR_REBOOT
            !snapshot.bootCompleted -> ReconciliationStatus.BOOT_IN_PROGRESS
            !snapshot.settings.matches(pending.desired) -> ReconciliationStatus.STATE_MISMATCH
            else -> ReconciliationStatus.VERIFIED
        }
        if (status == ReconciliationStatus.VERIFIED) {
            try { store.clear() } catch (_: Exception) {
                fail(Failure.STORAGE_FAILED, "Risultato letto dopo l'avvio, ma impossibile chiudere l'operazione salvata. Aggiorna nuovamente lo stato.")
            }
            onProgress(OperationStage.VERIFIED)
        }
        Reconciliation(snapshot, status)
    }

    private fun inspectInternal(expected: Identity?): Snapshot {
        val serial = scalar("getprop ro.serialno")
        if (!serial.matches(Regex("[A-Za-z0-9._-]{4,128}")) || serial.lowercase() in setOf("unknown", "null", "none", "00000000")) {
            fail(Failure.IDENTITY_UNAVAILABLE, "Identità univoca della Chromecast non disponibile. Nessuna modifica consentita.")
        }
        val identity = Identity(
            serial = serial,
            model = scalar("getprop ro.product.model"),
            device = scalar("getprop ro.product.device"),
            sdk = scalar("getprop ro.build.version.sdk").toIntOrNull() ?: -1,
            release = scalar("getprop ro.build.version.release"),
        )
        if (expected != null && identity != expected) {
            fail(Failure.WRONG_TARGET, "Il dispositivo non corrisponde alla Chromecast associata. Nessuna modifica consentita.")
        }
        if (identity.device != "boreal" || identity.sdk != 34 || identity.release != "14") {
            fail(Failure.UNSUPPORTED_TARGET, "Configurazione supportata: Chromecast HD con Android 14. Nessuna modifica consentita su questo dispositivo.")
        }
        if (scalar("am get-current-user") != "0") {
            fail(Failure.WRONG_USER, "È attivo un profilo Android diverso da quello previsto. Torna al profilo principale della Chromecast.")
        }
        val packagePaths = read("pm path $PROTON_PACKAGE").trimEnd('\r', '\n')
        if (packagePaths.isEmpty() || packagePaths.lines().any { !it.matches(Regex("package:/[^\\r\\n]+\\.apk")) }) {
            fail(Failure.PROTON_MISSING, "Proton VPN non risulta installata sulla Chromecast. Installala e configurala dal televisore.")
        }
        val settings = VpnSettings(
            app = scalar("settings --user 0 get secure always_on_vpn_app"),
            lockdown = scalar("settings --user 0 get secure always_on_vpn_lockdown"),
            whitelist = scalar("settings --user 0 get secure always_on_vpn_lockdown_whitelist", allowBlank = true),
        )
        val bootId = try {
            scalar("cat /proc/sys/kernel/random/boot_id").lowercase().takeIf {
                it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            }
        } catch (_: OperationException) { null }
        val bootCompleted = try { scalar("getprop sys.boot_completed") == "1" } catch (_: OperationException) { false }
        return Snapshot(identity, settings, bootId, bootCompleted)
    }

    private fun scalar(command: String, allowBlank: Boolean = false): String {
        val raw = read(command)
        val result = raw.removeSuffix("\n").removeSuffix("\r")
        if (raw.isEmpty() || (!allowBlank && result.isBlank()) || result.any { it == '\n' || it == '\r' || it == '\u0000' } || result != result.trim()) {
            fail(Failure.READ_FAILED, "Risposta incompleta o inattesa dalla Chromecast. Stato da verificare: riprova la lettura.")
        }
        return result
    }

    private fun read(command: String): String = try {
        session.shell(command)
    } catch (_: Exception) {
        fail(Failure.READ_FAILED, "Lettura della Chromecast non riuscita. Ripristina il collegamento e riprova.")
    }

    private fun persist(pending: PendingOperation) {
        try { store.write(pending) } catch (_: Exception) {
            fail(Failure.STORAGE_FAILED, "Impossibile salvare l'avanzamento sul telefono. Operazione arrestata; aggiorna lo stato prima di riprovare.")
        }
    }

    private inline fun <T> exclusive(block: () -> T): T {
        if (!busy.compareAndSet(false, true)) fail(Failure.BUSY, "È già in corso un'operazione. Attendi il risultato.")
        return try { block() } finally { busy.set(false) }
    }

    companion object {
        fun commands(desired: DesiredState): List<String> = when (desired) {
            DesiredState.ON -> listOf(
                "settings --user 0 put secure always_on_vpn_app $PROTON_PACKAGE",
                "settings --user 0 put secure always_on_vpn_lockdown 1",
            )
            DesiredState.OFF -> listOf(
                "settings --user 0 delete secure always_on_vpn_lockdown",
                "settings --user 0 delete secure always_on_vpn_app",
                "settings --user 0 delete secure always_on_vpn_lockdown_whitelist",
            )
        }

        private fun fail(failure: Failure, message: String): Nothing = throw OperationException(failure, message)
    }
}
