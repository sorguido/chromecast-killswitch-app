# Provenance and local changes

Source: [LibADB Android 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), by Muntashir Al-Islam. Inspected/imported on 24 October 2026. I retain upstream SPDX identifiers and license texts; for dual-licensed files I select Apache-2.0 without changing other files' licenses.

Original archive: `https://github.com/MuntashirAkon/libadb-android/archive/refs/tags/3.1.1.tar.gz`, SHA-256 `3afbe40b278ddea4480ebb8e7b707404f827d8e41b383e12f4b1b0b82dab1d84`.

Imported `libadb/src/main` and `COPYING`/`LICENSES`; the Gradle module is local. SPAKE2, HKDF, AES-GCM and the pairing protocol remain upstream code.

Changes of 24 October 2026:

- `AdbConnection`: TCP connection deadline of 6 seconds; TCP/TLS read deadline of 15 seconds; close socket on constructor failure; limit close-time thread wait to 2 seconds. Reject AUTH/CNXN without TLS and bound negotiated payload to client capacity. Remove exception stack traces that could expose endpoints.
- `PairingConnectionCtx`: cancellable socket, 6-second connection/12-second read deadlines and null-safe close. Separate `destroy()` from cancellation to avoid concurrent SPAKE2 context destruction. No PeerInfo logging. Expose only the GUID received after successful PAKE, validate AOSP discriminator `ADB_DEVICE_GUID=1` and the NUL-terminated string. The app derives the serial from the GUID following AOSP `GenerateDeviceGuid()` without reading a shell-protected property.
- `SslUtils`: create TLS context for the current key instead of statically caching a previous identity; remove stdout message. The upstream TLS trust policy remains unchanged: it does not strongly authenticate the server against an active LAN attacker. The [operation contract](../../docs/COMMAND_CONTRACT.md#target-and-preflight) documents the trusted-LAN requirement and limitations of saved identity checks.
- `AdbInputStream`/`AdbStream`: drain already received bytes even after CLSE, return EOF without an indefinite empty-queue wait when close is pending, and prevent sign extension of single-byte reads.

The app uses `AdbConnection.Builder`, `PairingConnectionCtx` and `open("reboot:")`. It does not use upstream automatic-connection helpers that may select the first discovered device. The build's `spake2Aar` property supports relinking with a modified LGPL library; see [SPAKE2 source](../spake2-source/README.md).
