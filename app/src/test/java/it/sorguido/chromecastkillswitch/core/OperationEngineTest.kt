package it.sorguido.chromecastkillswitch.core

import org.junit.Assert.*
import org.junit.Test

class OperationEngineTest {
    private val identity = Identity("SYNTHETIC-0001", "Chromecast", "boreal", 34, "14")
    private val bootA = "00000000-0000-0000-0000-000000000001"
    private val bootB = "00000000-0000-0000-0000-000000000002"

    private inner class MemoryStore : PendingStore {
        var pending: PendingOperation? = null
        var failWrites = false
        var failClear = false
        val phases = mutableListOf<OperationStage>()
        override fun read() = pending
        override fun write(operation: PendingOperation) {
            if (failWrites) error("synthetic persistence failure")
            pending = operation
            phases += operation.phase
        }
        override fun clear() {
            if (failClear) error("synthetic clear failure")
            pending = null
        }
    }

    private inner class Device(private val store: MemoryStore) : CommandSession {
        var app = "null"
        var lockdown = "null"
        var whitelist = "null"
        var serial = identity.serial
        var device = identity.device
        var sdk = "34"
        var currentUser = "0"
        var protonPath = "package:/data/app/synthetic/base.apk\n"
        var bootId = bootA
        var bootCompleted = "1"
        var reject: String? = null
        var rebootFailure = false
        var ignoreWrites = false
        var duringWrite: (() -> Unit)? = null
        var scalarOverrides = mutableMapOf<String, String>()
        val mutations = mutableListOf<String>()
        var reboots = 0
        override fun shell(command: String): String {
            if (command == reject) error("synthetic shell failure")
            scalarOverrides[command]?.let { return it }
            return when (command) {
                "getprop ro.serialno" -> "$serial\n"
                "getprop ro.product.model" -> "${identity.model}\n"
                "getprop ro.product.device" -> "$device\n"
                "getprop ro.build.version.sdk" -> "$sdk\n"
                "getprop ro.build.version.release" -> "14\n"
                "am get-current-user" -> "$currentUser\n"
                "pm path $PROTON_PACKAGE" -> protonPath
                "settings --user 0 get secure always_on_vpn_app" -> "$app\n"
                "settings --user 0 get secure always_on_vpn_lockdown" -> "$lockdown\n"
                "settings --user 0 get secure always_on_vpn_lockdown_whitelist" -> "$whitelist\n"
                "cat /proc/sys/kernel/random/boot_id" -> "$bootId\n"
                "getprop sys.boot_completed" -> "$bootCompleted\n"
                else -> {
                    assertNotNull("Pending must be durable before any mutation", store.pending)
                    mutations += command
                    duringWrite?.invoke()
                    if (!ignoreWrites) when (command) {
                        "settings --user 0 put secure always_on_vpn_app $PROTON_PACKAGE" -> app = PROTON_PACKAGE
                        "settings --user 0 put secure always_on_vpn_lockdown 1" -> lockdown = "1"
                        "settings --user 0 delete secure always_on_vpn_app" -> app = "null"
                        "settings --user 0 delete secure always_on_vpn_lockdown" -> lockdown = "null"
                        "settings --user 0 delete secure always_on_vpn_lockdown_whitelist" -> whitelist = "null"
                        else -> error("Unexpected shell command: $command")
                    }
                    if (command.contains(" delete ")) "Deleted 0 rows\n" else ""
                }
            }
        }
        override fun reboot() {
            assertEquals(OperationStage.REBOOT_REQUESTED, store.pending?.phase)
            reboots++
            if (rebootFailure) error("synthetic disconnect during reboot")
        }
    }

    private fun failure(expected: Failure, action: () -> Unit) {
        try {
            action()
            fail("Expected $expected")
        } catch (e: OperationException) {
            assertEquals(expected, e.failure)
        }
    }

    @Test fun configurationSeparatesPartialAndConflictFromOff() {
        assertEquals(Configuration.ON, VpnSettings(PROTON_PACKAGE, "1", "null").configuration)
        assertEquals(Configuration.OFF, VpnSettings("null", "0", "").configuration)
        assertEquals(Configuration.OFF, VpnSettings("null", "null", "null").configuration)
        for (lockdown in listOf("0", "null")) {
            assertEquals(Configuration.PARTIAL, VpnSettings(PROTON_PACKAGE, lockdown, "null").configuration)
        }
        assertEquals(Configuration.PARTIAL, VpnSettings("null", "1", "null").configuration)
        assertEquals(Configuration.CONFLICT, VpnSettings("other.provider", "1", "null").configuration)
        assertEquals(Configuration.CONFLICT, VpnSettings(PROTON_PACKAGE, "1", "other.app").configuration)
        assertEquals(Configuration.CONFLICT, VpnSettings(PROTON_PACKAGE, "11", "null").configuration)
    }

    @Test fun onIsNotSuccessfulUntilNewCompletedBootIsRead() {
        val store = MemoryStore()
        val remote = Device(store)
        val engine = OperationEngine(remote, store)
        engine.execute(DesiredState.ON, identity)
        assertEquals(OperationEngine.commands(DesiredState.ON), remote.mutations)
        assertEquals(1, remote.reboots)
        assertEquals(ReconciliationStatus.WAITING_FOR_REBOOT, engine.reconcile(identity).status)
        remote.bootId = bootB
        remote.bootCompleted = "0"
        assertEquals(ReconciliationStatus.BOOT_IN_PROGRESS, engine.reconcile(identity).status)
        remote.bootCompleted = "1"
        assertEquals(ReconciliationStatus.VERIFIED, engine.reconcile(identity).status)
        assertNull(store.pending)
    }

    @Test fun offRemovesAllThreeKeysEvenFromPartialState() {
        val store = MemoryStore()
        val remote = Device(store).apply { app = PROTON_PACKAGE; lockdown = "0" }
        val engine = OperationEngine(remote, store)
        assertEquals(Configuration.PARTIAL, engine.inspect(identity).configuration)
        engine.execute(DesiredState.OFF, identity)
        assertEquals(OperationEngine.commands(DesiredState.OFF), remote.mutations)
        assertEquals(1, remote.reboots)
        remote.bootId = bootB
        remote.lockdown = "0" // Android può ricreare il valore predefinito dopo l'avvio.
        assertEquals(ReconciliationStatus.VERIFIED, engine.reconcile(identity).status)
    }

    @Test fun offRequiresAllNullBeforeRebootRatherThanJustAnOffClassification() {
        val store = MemoryStore()
        val remote = Device(store).apply { lockdown = "0"; ignoreWrites = true }
        failure(Failure.READBACK_FAILED) { OperationEngine(remote, store).execute(DesiredState.OFF, identity) }
        assertEquals(0, remote.reboots)
        assertNotNull(store.pending)
    }

    @Test fun foreignTargetCannotBeMutatedEvenWithSameModel() {
        val store = MemoryStore()
        val remote = Device(store).apply { serial = "SYNTHETIC-0002" }
        failure(Failure.WRONG_TARGET) { OperationEngine(remote, store).execute(DesiredState.ON, identity) }
        assertTrue(remote.mutations.isEmpty())
        assertNull(store.pending)
    }

    @Test fun preflightRejectsWrongUserUnsupportedTargetMissingProtonAndForeignVpn() {
        val preparations: List<Pair<Failure, Device.() -> Unit>> = listOf(
            Failure.WRONG_USER to { currentUser = "10" },
            Failure.UNSUPPORTED_TARGET to { sdk = "35" },
            Failure.PROTON_MISSING to { protonPath = "" },
            Failure.CONFIGURATION_CONFLICT to { app = "other.provider" },
            Failure.CONFIGURATION_CONFLICT to { whitelist = "allowed.app" },
        )
        for ((expectedFailure, prepare) in preparations) {
            val store = MemoryStore()
            val remote = Device(store).apply(prepare)
            val expectedIdentity = if (expectedFailure == Failure.UNSUPPORTED_TARGET) identity.copy(sdk = 35) else identity
            failure(expectedFailure) { OperationEngine(remote, store).execute(DesiredState.ON, expectedIdentity) }
            assertTrue(remote.mutations.isEmpty())
            assertEquals(0, remote.reboots)
        }
    }

    @Test fun emptyTruncatedAndMultilineReadNeverBecomeOff() {
        for (output in listOf("", "\n", "null\nnull\n", " null\n", "null\u0000\n")) {
            val store = MemoryStore()
            val remote = Device(store).apply {
                scalarOverrides["settings --user 0 get secure always_on_vpn_app"] = output
            }
            failure(Failure.READ_FAILED) { OperationEngine(remote, store).inspect(identity) }
            assertTrue(remote.mutations.isEmpty())
        }
    }

    @Test fun blankWhitelistNeedsACompleteLineNotMissingOutput() {
        val store = MemoryStore()
        val remote = Device(store).apply { whitelist = "" }
        assertEquals(Configuration.OFF, OperationEngine(remote, store).inspect(identity).configuration)
        remote.scalarOverrides["settings --user 0 get secure always_on_vpn_lockdown_whitelist"] = ""
        failure(Failure.READ_FAILED) { OperationEngine(remote, store).inspect(identity) }
    }

    @Test fun partialWriteFailureDoesNotRebootOrAutomaticallyUndoProtection() {
        val store = MemoryStore()
        val remote = Device(store).apply { reject = OperationEngine.commands(DesiredState.ON)[1] }
        val engine = OperationEngine(remote, store)
        failure(Failure.WRITE_FAILED) { engine.execute(DesiredState.ON, identity) }
        assertEquals(listOf(OperationEngine.commands(DesiredState.ON)[0]), remote.mutations)
        assertEquals(Configuration.PARTIAL, engine.inspect(identity).configuration)
        assertEquals(0, remote.reboots)
        assertEquals(OperationStage.WRITING, store.pending?.phase)
    }

    @Test fun ignoredWriteOrFailedReadbackPreventsReboot() {
        for (failReadback in listOf(false, true)) {
            val store = MemoryStore()
            val remote = Device(store).apply {
                ignoreWrites = !failReadback
                if (failReadback) duringWrite = { reject = "settings --user 0 get secure always_on_vpn_app" }
            }
            failure(Failure.READBACK_FAILED) { OperationEngine(remote, store).execute(DesiredState.ON, identity) }
            assertEquals(0, remote.reboots)
        }
    }

    @Test fun persistenceFailureBeforeFirstWriteCannotTouchTarget() {
        val store = MemoryStore().apply { failWrites = true }
        val remote = Device(store)
        failure(Failure.STORAGE_FAILED) { OperationEngine(remote, store).execute(DesiredState.ON, identity) }
        assertTrue(remote.mutations.isEmpty())
        assertEquals(0, remote.reboots)
    }

    @Test fun rebootDisconnectSurvivesProcessRestartWithoutReplay() {
        val store = MemoryStore()
        val remote = Device(store).apply { rebootFailure = true }
        failure(Failure.REBOOT_UNCERTAIN) { OperationEngine(remote, store).execute(DesiredState.ON, identity) }
        assertEquals(OperationStage.REBOOT_REQUESTED, store.pending?.phase)
        val restarted = OperationEngine(remote, store)
        assertEquals(ReconciliationStatus.WAITING_FOR_REBOOT, restarted.reconcile(identity).status)
        assertEquals(1, remote.reboots)
        assertEquals(2, remote.mutations.size)
        remote.bootId = bootB
        assertEquals(ReconciliationStatus.VERIFIED, restarted.reconcile(identity).status)
        assertEquals(1, remote.reboots)
        assertEquals(2, remote.mutations.size)
    }

    @Test fun changedBootWithWrongStateDoesNotClearPendingOrReplay() {
        val store = MemoryStore()
        val remote = Device(store)
        val engine = OperationEngine(remote, store)
        engine.execute(DesiredState.ON, identity)
        remote.bootId = bootB
        remote.lockdown = "0"
        assertEquals(ReconciliationStatus.STATE_MISMATCH, engine.reconcile(identity).status)
        assertNotNull(store.pending)
        assertEquals(1, remote.reboots)
    }

    @Test fun unavailableBootEvidenceBlocksMutationAndRetainsPendingOnReconcile() {
        val store = MemoryStore()
        val remote = Device(store).apply { bootId = "" }
        val engine = OperationEngine(remote, store)
        assertEquals(Configuration.OFF, engine.inspect(identity).configuration)
        failure(Failure.BOOT_UNVERIFIABLE) { engine.execute(DesiredState.ON, identity) }
        assertTrue(remote.mutations.isEmpty())
        store.pending = PendingOperation(identity.serial, DesiredState.ON, bootA, OperationStage.REBOOT_REQUESTED)
        assertEquals(ReconciliationStatus.WAITING_FOR_REBOOT, engine.reconcile(identity).status)
        assertNotNull(store.pending)
    }

    @Test fun nestedOperationIsRejectedBeforeSecondMutation() {
        val store = MemoryStore()
        val remote = Device(store)
        val engine = OperationEngine(remote, store)
        remote.duringWrite = { failure(Failure.BUSY) { engine.execute(DesiredState.OFF, identity) } }
        engine.execute(DesiredState.ON, identity)
        assertEquals(OperationEngine.commands(DesiredState.ON), remote.mutations)
        assertEquals(1, remote.reboots)
    }

    @Test fun failureToClearPendingDoesNotReportCompletedOperation() {
        val store = MemoryStore()
        val remote = Device(store)
        val engine = OperationEngine(remote, store)
        engine.execute(DesiredState.ON, identity)
        remote.bootId = bootB
        store.failClear = true
        failure(Failure.STORAGE_FAILED) { engine.reconcile(identity) }
        assertNotNull(store.pending)
    }

    @Test fun interruptedWriteAndUnrequestedRebootDoNotBecomeCompletedCycle() {
        for (phase in listOf(OperationStage.WRITING, OperationStage.SAVED)) {
            val store = MemoryStore().apply {
                pending = PendingOperation(identity.serial, DesiredState.ON, bootA, phase)
            }
            val remote = Device(store).apply { app = PROTON_PACKAGE; lockdown = "1"; bootId = bootB }
            assertEquals(ReconciliationStatus.REQUIRES_EXPLICIT_RETRY, OperationEngine(remote, store).reconcile(identity).status)
            assertNotNull(store.pending)
            assertTrue(remote.mutations.isEmpty())
            assertEquals(0, remote.reboots)
        }
    }

    @Test fun explicitNewOperationCanReplacePendingAfterFreshPreflight() {
        val store = MemoryStore().apply {
            pending = PendingOperation(identity.serial, DesiredState.ON, bootA, OperationStage.WRITING)
        }
        val remote = Device(store).apply { app = PROTON_PACKAGE }
        val result = OperationEngine(remote, store).execute(DesiredState.OFF, identity)
        assertEquals(DesiredState.OFF, result.desired)
        assertEquals(OperationEngine.commands(DesiredState.OFF), remote.mutations)
        assertEquals(1, remote.reboots)
    }
}
