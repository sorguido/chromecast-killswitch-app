# ADB and VPN operation contract

This document owns commands, interpretation and sequencing. [PRODUCT_SPEC.md](PRODUCT_SPEC.md) owns product/UI requirements and [ROADMAP.md](../ROADMAP.md) owns delivery gates. Public primary references are routed through [CORROBORATIONS.md](../CORROBORATIONS.md).

## Target and preflight

Supported target: Chromecast with Google TV HD, product/device `boreal`, Android 14/API 34, Android user `0`, Proton package `ch.protonvpn.android`. Model/firmware identify the supported profile, not a unique device.

The phone generates and stores its own ADB credentials in private, backup-excluded storage. Pairing uses the code shown by the Chromecast; no PC identity is copied. Reinstallation, clearing app data or authorization revocation may require pairing again. Corrupt identity storage must not silently generate a replacement identity.

Discover `_adb-tls-pairing._tcp` for pairing and `_adb-tls-connect._tcp` for connection. IP, port and mDNS names are endpoints, not authentication. Never automatically select the first discovered device. The authenticated PAKE GUID is stored privately; its AOSP device serial is checked against `ro.serialno` on every connection. Unsupported GUID formats, unreadable/mismatched serials or an unrelated saved identity block access.

The upstream TLS client accepts server certificates without a trusted server identity. PAKE authenticates pairing and the authorized key authenticates the phone to adbd, but shell identity checks do not establish strong server authentication against an active LAN attacker. Use a trusted LAN, do not expose ADB to the Internet, and do not claim MITM resistance. See [local transport changes](../vendor/libadb/LOCAL_CHANGES.md).

After identity checks, read preflight state with fixed remote-shell commands:

```sh
getprop ro.product.model
getprop ro.product.device
getprop ro.build.version.release
getprop ro.build.version.sdk
am get-current-user
pm path ch.protonvpn.android
settings --user 0 get secure always_on_vpn_app
settings --user 0 get secure always_on_vpn_lockdown
settings --user 0 get secure always_on_vpn_lockdown_whitelist
```

The Android app opens ADB services directly; it does not invoke a PC binary. Remote daemon API parameters follow the supported TV, not the phone OS. Unconfirmed identity, unsupported target, another Android user, absent Proton, another always-on provider or unexpected whitelist entries block writes with a clear result. The supported baseline has no whitelist exceptions; do not overwrite unrelated configuration.

## Read state

Keep connection status, persistent values and operation progress separate. Accept `null` only from a successful framed read. Empty/truncated output, timeout or command failure does not mean an absent setting.

| Successful read for user 0 | Meaning |
| --- | --- |
| Proton app, lockdown `1`, no exceptions | System configuration ON |
| App `null`, lockdown `null` or `0`, whitelist absent/empty | System configuration OFF |
| Proton with lockdown `0`/`null`, or another incomplete combination | Partial configuration |
| Another provider, unexpected exceptions or unsupported values | Conflict; no automatic writes |
| Read failed, unverified target or unreachable endpoint | Status needs verification |

Settings do not measure traffic. Never infer “no leaks” or “tunnel connected”. The UI may distinguish a current read from post-restart verification and show the last-read time. Proton Auto-connect is independent: OFF neither necessarily disconnects the tunnel nor changes Proton's internal preference.

## Enable

Only on the verified target, one operation at a time and after UI confirmation:

```sh
settings --user 0 put secure always_on_vpn_app ch.protonvpn.android
settings --user 0 put secure always_on_vpn_lockdown 1
```

Read all three keys again. Require exactly Proton/`1` and no whitelist exceptions. A favorable exit code without readback is insufficient. A failed or partial write blocks automatic restart and requires an explicit state and pertinent recovery. Failed ON must not automatically invoke OFF and weaken a pre-existing protection.

## Disable completely

Apply in this order:

```sh
settings --user 0 delete secure always_on_vpn_lockdown
settings --user 0 delete secure always_on_vpn_app
settings --user 0 delete secure always_on_vpn_lockdown_whitelist
```

Read all three keys and require `null` before restart. Deleting an absent key is valid: `Deleted 0 rows` is not failure. Do not use `put ... null`, which stores a string. OFF also works when only always-on is enabled and lockdown is already disabled. On discordant results, do not claim success or restart automatically. Preserve the minimum pending state without logging secrets.

## Restart and reconnect

After favorable readback, use ADB's `reboot:` service, equivalent to `adb reboot`; do not substitute an arbitrary shell string. Persist the pending operation before mutation/restart as implemented. Connection loss is expected but does not prove a completed restart.

Rediscover, recheck identity and wait for available startup evidence (`getprop sys.boot_completed` and a comparable boot indicator where readable). Use finite timeouts/bounded retries. Never loop mutations or restarts because a reply was lost.

After startup, verify Proton/`1`/no exceptions for ON; app `null`, lockdown `null` or `0`, whitelist absent/empty for OFF. Runtime signals can supplement readback only with documented scope. These checks do not certify every traffic path or replace actual functional testing.

Backgrounding, rotation, language changes or process death must not erase pending-operation meaning. On return, read/reconcile instead of replaying writes or restarts. No permanent service is required.

## Transport errors and safe behavior

Distinguish pairing rejection/expiry, revoked credentials, empty discovery, changed endpoint, network/permission failure, unsupported target, shell failure, partial configuration and unverified restart. Keep typed failure reasons separate from language-dependent display text.

Use only allowlisted commands and validated arguments. Never interpolate pairing codes, UI text or endpoints into remote commands. Shell frames use random delimiters, checked exit codes and bounded output; a loose occurrence of `1` is not success. Transport deadlines and cancellation bound pairing, connection and command waits. Concurrent double taps are blocked.

A popup informs and confirms; it cannot enable a stopped adbd. If Wireless debugging is off, show TV-side instructions and retry. Manual IP entry only helps when a reachable endpoint exists.

## USB recovery

USB is a recovery option **only if USB debugging is already enabled and the PC is authorized**. Use a suitable data connection and power supply. Verify the target's identity, supported model/API and Android user before any write. `adb -d` selects the sole USB device and avoids accidentally choosing a wireless connection; if several USB devices are present, select the verified serial explicitly instead.

With Android SDK Platform Tools on `PATH`, inspect the target first:

```sh
adb -d shell getprop ro.product.device
adb -d shell getprop ro.build.version.sdk
adb -d shell am get-current-user
adb -d shell pm path ch.protonvpn.android
adb -d shell settings --user 0 get secure always_on_vpn_app
adb -d shell settings --user 0 get secure always_on_vpn_lockdown
adb -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist
```

Proceed only for the verified supported target with the expected Proton configuration and no unrelated provider or whitelist exceptions. The following deliberately removes Android always-on/lockdown and restarts only after all three deletions are read back:

```bash
if adb -d shell settings --user 0 delete secure always_on_vpn_lockdown &&
   adb -d shell settings --user 0 delete secure always_on_vpn_app &&
   adb -d shell settings --user 0 delete secure always_on_vpn_lockdown_whitelist &&
   [ "$(adb -d shell settings --user 0 get secure always_on_vpn_app | tr -d '\r\n')" = "null" ] &&
   [ "$(adb -d shell settings --user 0 get secure always_on_vpn_lockdown | tr -d '\r\n')" = "null" ] &&
   [ "$(adb -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist | tr -d '\r\n')" = "null" ]; then
    echo "System configuration removed. Restarting the Chromecast..."
    adb -d reboot
else
    echo "Removal not verified. Check the cable, USB debugging and PC authorization."
fi
```

After restart, read the three keys again using the same verified target and apply the OFF interpretation above. This is technical recovery, not a prerequisite for normal app use.
