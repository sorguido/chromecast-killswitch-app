package it.sorguido.chromecastkillswitch.transport

import android.content.Context
import android.util.AtomicFile
import java.io.File
import io.github.muntashirakon.adb.AdbConnection
import io.github.muntashirakon.adb.PairingConnectionCtx
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class TransportReason { GUID_MISMATCH, PAIRING_FAILED, NOT_PAIRED, CONNECTION_FAILED, IDENTITY_STORAGE }

/** Messages are fixed Italian strings; never attach upstream exceptions or wire data. */
class TransportException(val reason: TransportReason, message: String) : IOException(message)

/** Blocking API: invoke only from the controller's serial IO worker. Never logs wire data. */
class AdbTransport(private val context: Context) {
    private val peer = AtomicFile(File(context.noBackupFilesDir, "adb-peer-guid"))

    private fun identity(): AdbIdentity = try { AdbIdentity.load(context) }
    catch (_: Exception) { throw TransportException(TransportReason.IDENTITY_STORAGE,
        "Identità ADB non leggibile. Consulta la guida di recupero prima di cancellare i dati dell’app.") }

    private fun savedGuid(): String? = try {
        if (peer.baseFile.exists() || File(peer.baseFile.path + ".bak").exists()) {
            peer.readFully().toString(Charsets.US_ASCII).also {
                check(it.matches(Regex("[A-Za-z0-9._-]{1,128}")))
            }
        } else null
    } catch (_: Exception) { throw TransportException(TransportReason.IDENTITY_STORAGE,
        "Associazione salvata non leggibile. Consulta la guida di recupero.") }

    fun matchesPairedService(endpoint: Endpoint): Boolean {
        val guid = savedGuid() ?: return false
        return endpoint.kind == EndpointKind.CONNECT && endpoint.serviceName.equals(guid, ignoreCase = true)
    }

    private fun retainGuid(guid: String) {
        PeerBinding.serialFromGuid(guid)
        val previous = savedGuid()
        if (previous != null && previous != guid) throw TransportException(TransportReason.GUID_MISMATCH,
            "Il codice appartiene a un altro dispositivo. La Chromecast già associata resta selezionata.")
        if (previous == guid) return
        var stream: java.io.FileOutputStream? = null
        try {
            stream = peer.startWrite()
            stream.write(guid.toByteArray(Charsets.US_ASCII))
            peer.finishWrite(stream)
        } catch (failure: Exception) {
            peer.failWrite(stream)
            throw TransportException(TransportReason.IDENTITY_STORAGE, "Impossibile conservare l’associazione. Nessuna operazione è stata avviata.")
        }
    }

    fun pair(endpoint: Endpoint, code: String) {
        require(endpoint.kind == EndpointKind.PAIRING) { "Seleziona la porta di associazione." }
        require(code.matches(Regex("[0-9]{6}"))) { "Inserisci il codice di sei cifre mostrato sul TV." }
        val identity = identity()
        val password = code.toByteArray(Charsets.UTF_8)
        val pairing = try { PairingConnectionCtx(endpoint.host, endpoint.port, password,
            identity.key, identity.certificate, "ChromecastKillSwitch") }
        catch (_: Exception) {
            password.fill(0)
            throw TransportException(TransportReason.PAIRING_FAILED, "Associazione non avviata. Controlla il codice e la porta sul TV e riprova.")
        }
        val deadline = deadlines.schedule({ pairing.close() }, 30, TimeUnit.SECONDS)
        try {
            pairing.start()
            retainGuid(pairing.peerGuid)
        } catch (failure: TransportException) { throw failure }
        catch (_: SecurityException) { throw TransportException(TransportReason.CONNECTION_FAILED,
            "Accesso alla rete negato. Controlla i permessi dell’app nelle impostazioni del telefono.") }
        catch (_: Exception) { throw TransportException(TransportReason.PAIRING_FAILED,
            "Associazione rifiutata, scaduta o non raggiungibile. Apri un nuovo codice sul TV e verifica la porta di associazione.") }
        finally {
            deadline.cancel(false)
            pairing.destroy()
            password.fill(0)
        }
    }

    fun connect(endpoint: Endpoint): AdbSession {
        require(endpoint.kind == EndpointKind.CONNECT) { "Serve la porta di collegamento, diversa da quella di associazione." }
        val expectedGuid = savedGuid() ?: throw TransportException(TransportReason.NOT_PAIRED, "Associa prima il telefono usando il codice sul TV.")
        val expectedSerial = PeerBinding.serialFromGuid(expectedGuid)
        val identity = identity()
        val connection = try {
            AdbConnection.Builder(endpoint.host, endpoint.port)
                .setApi(34) // Target daemon: Chromecast Android 14, independent of this phone's API.
                .setPrivateKey(identity.key).setCertificate(identity.certificate)
                .setDeviceName("ChromecastKillSwitch").build()
        } catch (_: SecurityException) { throw TransportException(TransportReason.CONNECTION_FAILED,
            "Accesso alla rete negato. Controlla i permessi dell’app nelle impostazioni del telefono.") }
        catch (_: Exception) { throw TransportException(TransportReason.CONNECTION_FAILED,
            "Collegamento non raggiungibile. Verifica rete locale, Debug wireless e porta corrente sul TV.") }
        try {
            val deadline = deadlines.schedule({ runCatching { connection.close() } }, 12, TimeUnit.SECONDS)
            try {
                if (!connection.connect(12, TimeUnit.SECONDS, true)) throw IOException("Connection not confirmed")
            } finally { deadline.cancel(false) }
            val session = AdbSession(connection)
            val actualSerial = session.shell("getprop ro.serialno").removeSuffix("\n")
            if (actualSerial != expectedSerial) {
                throw TransportException(TransportReason.GUID_MISMATCH,
                    "Il seriale non corrisponde all’associazione o non è leggibile. Nessuna modifica è consentita. Controlla la Chromecast selezionata.")
            }
            return session
        } catch (failure: Exception) {
            runCatching { connection.close() }
            if (failure is TransportException) throw failure
            throw TransportException(TransportReason.CONNECTION_FAILED,
                "Collegamento non confermato. Verifica Debug wireless e porta corrente; se l’autorizzazione è stata revocata, associa nuovamente il telefono.")
        }
    }

    internal companion object {
        val deadlines = Executors.newScheduledThreadPool(2) { task ->
            Thread(task, "adb-deadline").apply { isDaemon = true }
        }
    }
}

class AdbSession internal constructor(private val connection: AdbConnection) : Closeable {
    @Synchronized fun shell(command: String): String {
        require(command in allowedCommands) { "Comando remoto non previsto." }
        val token = UUID.randomUUID().toString().replace("-", "")
        val begin = "__CKS_BEGIN_${token}__"
        val end = "__CKS_END_${token}__"
        // Fixed commands only. No UI input, endpoint or pairing secret is interpolated in shell.
        val script = "printf '$begin\\n'; $command 2>&1; cks_status=\$?; printf '\\n$end:%s\\n' \"\$cks_status\""
        val deadline = AdbTransport.deadlines.schedule({ runCatching { connection.close() } }, 15, TimeUnit.SECONDS)
        try {
            connection.open("shell:$script").use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                val input = stream.openInputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > 65_536) throw IOException("Risposta ADB troppo lunga.")
                    output.write(buffer, 0, count)
                }
                return ShellFrame.parse(output.toString("UTF-8"), begin, end)
            }
        } finally { deadline.cancel(false) }
    }

    @Synchronized fun reboot() {
        val deadline = AdbTransport.deadlines.schedule({ runCatching { connection.close() } }, 12, TimeUnit.SECONDS)
        try { connection.open("reboot:").use { /* Service request only; result requires boot verification. */ } }
        finally { deadline.cancel(false) }
    }

    override fun close() = connection.close()

    companion object {
        private val allowedCommands = setOf(
            "getprop ro.product.model", "getprop ro.product.device", "getprop ro.build.version.release",
            "getprop ro.build.version.sdk", "getprop ro.serialno", "getprop ro.boot.serialno",
            "getprop sys.boot_completed", "cat /proc/sys/kernel/random/boot_id", "am get-current-user",
            "pm path ch.protonvpn.android",
            "settings --user 0 get secure always_on_vpn_app",
            "settings --user 0 get secure always_on_vpn_lockdown",
            "settings --user 0 get secure always_on_vpn_lockdown_whitelist",
            "settings --user 0 put secure always_on_vpn_app ch.protonvpn.android",
            "settings --user 0 put secure always_on_vpn_lockdown 1",
            "settings --user 0 delete secure always_on_vpn_lockdown",
            "settings --user 0 delete secure always_on_vpn_app",
            "settings --user 0 delete secure always_on_vpn_lockdown_whitelist",
        )
    }
}

internal object ShellFrame {
    fun parse(raw: String, begin: String, end: String): String {
        val text = raw.replace("\r\n", "\n")
        val prefix = "$begin\n"
        if (!text.startsWith(prefix)) throw IOException("Risposta ADB senza inizio verificabile.")
        val marker = "\n$end:"
        val position = text.lastIndexOf(marker)
        if (position < prefix.length || text.indexOf(marker) != position) throw IOException("Risposta ADB incompleta.")
        val status = text.substring(position + marker.length)
        if (!status.matches(Regex("[0-9]{1,3}\n"))) throw IOException("Risposta ADB troncata o non valida.")
        if (status.trim().toInt() != 0) throw IOException("Il comando remoto non è riuscito. Rileggi lo stato prima di riprovare.")
        return text.substring(prefix.length, position)
    }
}

/** AOSP Android 14 daemon/mdns.cpp GenerateDeviceGuid(): adb-<ro.serialno>-<six alphanumerics>. */
internal object PeerBinding {
    fun serialFromGuid(guid: String): String {
        val serial = Regex("adb-([A-Za-z0-9._-]+)-[A-Za-z0-9]{6}").matchEntire(guid)?.groupValues?.get(1)
        if (serial.isNullOrEmpty() || serial.length > 117 || serial.lowercase() in setOf("unknown", "null")) {
            throw TransportException(TransportReason.GUID_MISMATCH,
                "L’identità ricevuta durante l’associazione non ha un formato supportato. Nessuna modifica è consentita.")
        }
        return serial
    }
}
