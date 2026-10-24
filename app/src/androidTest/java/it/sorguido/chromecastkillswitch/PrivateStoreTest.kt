// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import it.sorguido.chromecastkillswitch.core.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PrivateStoreTest {
    @Test fun interruptedAtomicWriteKeepsRecoverablePendingAndIdentity() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(base.cacheDir, "storage-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(base) { override fun getNoBackupFilesDir(): File = folder }
        try {
            val store = PrivateStore(isolated)
            val identity = Identity("synthetic-serial", "Test", "boreal", 34, "14")
            val pending = PendingOperation(identity.serial, DesiredState.ON,
                "00000000-0000-4000-8000-000000000001", OperationStage.REBOOT_REQUESTED)
            store.saveIdentity(identity)
            store.write(pending)
            assertTrue(File(folder, "pending.json").renameTo(File(folder, "pending.json.bak")))
            val reopened = PrivateStore(isolated)
            assertEquals(identity, reopened.identity())
            assertEquals(pending, reopened.read())
            File(folder, "pending.json.new").writeText("{incomplete")
            assertEquals(pending, PrivateStore(isolated).read())
            reopened.clear()
            assertNull(PrivateStore(isolated).read())
        } finally { folder.deleteRecursively() }
    }
}
