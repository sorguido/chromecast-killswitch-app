// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ShellFrameTest {
    @Test fun successfulNullIsNotAnEmptyResponse() {
        assertEquals("null\n", ShellFrame.parse("BEGIN\nnull\n\nEND:0\n", "BEGIN", "END"))
        assertEquals("\n", ShellFrame.parse("BEGIN\n\n\nEND:0\n", "BEGIN", "END"))
        assertEquals("", ShellFrame.parse("BEGIN\n\nEND:0\n", "BEGIN", "END"))
    }

    @Test fun rejectsMissingTruncatedDuplicateAndFailedFrames() {
        listOf("null", "BEGIN\nnull\n", "BEGIN\nnull\nEND:0", "BEGIN\nnull\nEND:1\n",
            "BEGIN\nnull\nEND:0\nextra", "BEGIN\nEND:0\nEND:0\n", "junkBEGIN\nnull\nEND:0\n")
            .forEach { raw -> assertThrows(IOException::class.java) { ShellFrame.parse(raw, "BEGIN", "END") } }
    }

    @Test fun normalizesOnlyTerminalLineEndings() {
        assertEquals("value\n", ShellFrame.parse("BEGIN\r\nvalue\r\n\r\nEND:0\r\n", "BEGIN", "END"))
    }

    @Test fun bindsSerialFromAuthenticatedPairingGuid() {
        assertEquals("target-serial-with-hyphens", PeerBinding.serialFromGuid("adb-target-serial-with-hyphens-aB2xY9"))
        assertEquals("Serial_012.3", PeerBinding.serialFromGuid("adb-Serial_012.3-012345"))
        listOf("", "random-guid", "adb--abcdef", "adb-unknown-abcdef", "adb-null-abcdef",
            "adb-target-abcde", "adb-target-abcdefg", "adb-target-abcde!", "prefix-adb-target-abcdef", "adb-target-abcdef\n")
            .forEach { guid -> assertThrows(TransportException::class.java) { PeerBinding.serialFromGuid(guid) } }
    }

    @Test fun parsesOnlyNumericEndpoints() {
        assertEquals("192.168.1.8:37001", Endpoint.parse("192.168.1.8", "37001").address)
        listOf("", "example.org", "1.2.3.999", "1.2.3.4;reboot", "01.2.3.4")
            .forEach { host -> assertThrows(IllegalArgumentException::class.java) { Endpoint.parse(host, "1") } }
        listOf("0", "65536", "-1", "port")
            .forEach { port -> assertThrows(IllegalArgumentException::class.java) { Endpoint.parse("1.2.3.4", port) } }
    }
}
