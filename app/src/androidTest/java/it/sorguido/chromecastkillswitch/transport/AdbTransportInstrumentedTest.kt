package it.sorguido.chromecastkillswitch.transport

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.conscrypt.Conscrypt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/** Only emulator loopback is accepted. No Chromecast writes, reboot or physical-device target. */
@RunWith(AndroidJUnit4::class)
class AdbTransportInstrumentedTest {
    @Test fun privateIdentitySurvivesReloadAndBundledTlsLoads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = AdbIdentity.load(context)
        val second = AdbIdentity.load(context)
        assertTrue(MessageDigest.isEqual(first.key.encoded, second.key.encoded))
        assertTrue(MessageDigest.isEqual(first.certificate.encoded, second.certificate.encoded))
        assertTrue(Conscrypt.isAvailable())
        assertTrue(context.noBackupFilesDir.resolve("adb-identity.bin").isFile)
    }

    @Test fun optInRealPairingAndReconnectOnEmulator() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Prova pairing non richiesta", args.getString("transportTestEnabled") == "true")
        assertTrue("Questa prova è consentita soltanto su un emulatore locale.",
            Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        val pairingPort = args.getString("pairingPort")?.toIntOrNull()
        val connectPort = args.getString("connectPort")?.toIntOrNull()
        val code = args.getString("pairingCode")
        assertTrue("Parametri di collaudo mancanti.", pairingPort != null && connectPort != null && code != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var phase = "CREATE_TRANSPORT"
        try {
            val transport = AdbTransport(context)
            phase = "PAIR"
            transport.pair(Endpoint("127.0.0.1", pairingPort!!, kind = EndpointKind.PAIRING), code!!)
            val endpoint = Endpoint("127.0.0.1", connectPort!!)
            phase = "CONNECT_AND_SERIAL"
            transport.connect(endpoint).use { session ->
                phase = "READ_SDK"
                assertEquals(Build.VERSION.SDK_INT.toString(), session.shell("getprop ro.build.version.sdk").trim())
            }
            // New transport instance reloads both credentials and the authenticated GUID.
            phase = "RECONNECT_AND_SERIAL"
            AdbTransport(context).connect(endpoint).use { session ->
                phase = "READ_BOOT_COMPLETED"
                assertEquals("1", session.shell("getprop sys.boot_completed").trim())
            }
        } catch (failure: Throwable) {
            // Only phase, class and fixed, sanitized TransportException messages may leave this test.
            val detail = if (failure is TransportException)
                "reason=${failure.reason}; message=${failure.message}" else "reason=UNCLASSIFIED"
            throw AssertionError("phase=$phase; class=${failure.javaClass.simpleName}; $detail")
        }
    }
}
