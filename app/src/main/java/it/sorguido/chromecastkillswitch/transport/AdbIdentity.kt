package it.sorguido.chromecastkillswitch.transport

import android.content.Context
import android.util.AtomicFile
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

internal data class AdbIdentity(val key: PrivateKey, val certificate: X509Certificate) {
    companion object {
        @Synchronized fun load(context: Context): AdbIdentity {
            val storage = AtomicFile(File(context.noBackupFilesDir, "adb-identity.bin"))
            if (storage.baseFile.exists() || File(storage.baseFile.path + ".bak").exists()) {
                val bytes = storage.readFully()
                try {
                    DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                        check(input.readInt() == 1) { "Identità ADB non valida. È necessaria una nuova associazione." }
                        val keySize = input.readInt()
                        check(keySize in 1..8192)
                        val keyBytes = ByteArray(keySize).also(input::readFully)
                        val key = try { KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes)) }
                        finally { keyBytes.fill(0) }
                        val certSize = input.readInt()
                        check(certSize in 1..8192)
                        val cert = CertificateFactory.getInstance("X.509").generateCertificate(
                            ByteArrayInputStream(ByteArray(certSize).also(input::readFully))) as X509Certificate
                        check(input.read() == -1)
                        cert.checkValidity()
                        return AdbIdentity(key, cert)
                    }
                } finally { bytes.fill(0) }
            }
            val random = SecureRandom()
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, random)
            val keys = generator.generateKeyPair()
            val provider = BouncyCastleProvider()
            val subject = X500Name("CN=Chromecast Kill Switch")
            val now = System.currentTimeMillis()
            val cert = JcaX509CertificateConverter().setProvider(provider).getCertificate(
                JcaX509v3CertificateBuilder(subject, BigInteger(160, random), Date(now - 86_400_000L),
                    Date(now + 20L * 365 * 86_400_000L), subject, keys.public)
                    .build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(keys.private)))
            val keyBytes = keys.private.encoded
            val payload = ByteArrayOutputStream()
            try {
                DataOutputStream(payload).use { out ->
                    out.writeInt(1)
                    out.writeInt(keyBytes.size); out.write(keyBytes)
                    out.writeInt(cert.encoded.size); out.write(cert.encoded)
                }
                val bytes = payload.toByteArray()
                var stream: java.io.FileOutputStream? = null
                try {
                    stream = storage.startWrite()
                    stream.write(bytes)
                    storage.finishWrite(stream)
                } catch (failure: Exception) {
                    storage.failWrite(stream)
                    throw failure
                } finally { bytes.fill(0) }
            } finally { keyBytes.fill(0) }
            return AdbIdentity(keys.private, cert)
        }
    }
}
