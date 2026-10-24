// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import android.content.Context
import android.util.AtomicFile
import it.sorguido.chromecastkillswitch.core.*
import org.json.JSONObject
import java.io.File

/** Storage privato atomico, escluso da backup; nessun dato sensibile nei log. */
class PrivateStore(context: Context) : PendingStore {
    private val root = context.noBackupFilesDir
    private fun readJson(name: String): JSONObject? {
        val file = AtomicFile(File(root, name))
        return try { file.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) } }
        catch (e: java.io.FileNotFoundException) {
            if (file.baseFile.exists() || File(root, "$name.bak").exists()) throw e
            null
        }
    }
    private fun writeJson(name: String, value: JSONObject) {
        val file = AtomicFile(File(root, name))
        val stream = file.startWrite()
        try { stream.write(value.toString().toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    @Synchronized fun identity(): Identity? = readJson("target.json")?.let {
        Identity(it.getString("serial"), it.getString("model"), it.getString("device"), it.getInt("sdk"), it.getString("release"))
    }
    @Synchronized fun saveIdentity(identity: Identity) = writeJson("target.json", JSONObject().apply {
        put("serial", identity.serial); put("model", identity.model); put("device", identity.device)
        put("sdk", identity.sdk); put("release", identity.release)
    })
    @Synchronized fun pairedHost(): String? = readJson("pairing.json")?.getString("host")
    @Synchronized fun savePairing(host: String) = writeJson("pairing.json", JSONObject().put("host", host))
    @Synchronized fun endpoint(): Pair<String, Int>? = readJson("endpoint.json")?.let { it.getString("host") to it.getInt("port") }
    @Synchronized fun saveEndpoint(host: String, port: Int) = writeJson("endpoint.json", JSONObject().put("host", host).put("port", port))
    @Synchronized override fun read(): PendingOperation? = readJson("pending.json")?.let {
        PendingOperation(it.getString("serial"), DesiredState.valueOf(it.getString("desired")), it.getString("boot"), OperationStage.valueOf(it.getString("phase")))
    }
    @Synchronized override fun write(operation: PendingOperation) = writeJson("pending.json", JSONObject().apply {
        put("serial", operation.targetSerial); put("desired", operation.desired.name)
        put("boot", operation.beforeBootId); put("phase", operation.phase.name)
    })
    @Synchronized override fun clear() {
        val file = AtomicFile(File(root, "pending.json"))
        file.delete()
        check(!file.baseFile.exists()) { "Salvataggio non riuscito" }
    }
}
