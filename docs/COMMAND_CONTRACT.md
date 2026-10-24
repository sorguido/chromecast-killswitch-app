# Troubleshooting and recovery

Use this guide if pairing, connection or an operation does not finish. Normal setup and daily use are in the [README](../README.md).

## Connection problems

| Problem | What to do |
| --- | --- |
| No Chromecast appears | Put the phone and TV on the same trusted network and enable Wireless debugging on the TV. Guest-network isolation can prevent discovery. Use **Connection and pairing** to enter the current IP address and port manually. |
| Pairing fails or the code expires | Open **Pair device with pairing code** on the TV again. Use the pairing port and fresh six-digit code while that screen stays open. |
| Pairing succeeds but connection fails | Return to the TV's main Wireless debugging screen and use its **connection port**, which differs from the pairing port. |
| Connection stops working after a restart | Check that Wireless debugging is still enabled on the TV. Discover or enter its current connection address and port, then tap **Read state again**. |
| The app rejects the TV or its settings | Check the supported configuration below. A different VPN provider or an unexpected exception list blocks changes; do not clear settings belonging to another setup. |
| Internet access stops while the kill switch is on | Open Proton VPN on the TV and restore its connection. If you want to remove the system settings, use **Disable and reboot** when ADB is reachable, or the USB procedure below. |
| Proton still connects after disabling | Android's system settings and Proton's own **Auto-connect** preference are separate. Change Auto-connect inside Proton if you also want to stop that behavior. |

The app cannot switch on a disabled ADB service remotely. Manual IP entry only helps when Wireless debugging is running and reachable.

## Understanding the result

| Status | Meaning |
| --- | --- |
| ON | Android's always-on VPN selects Proton, lockdown is enabled and no exception list is configured. |
| OFF | The system VPN selection and exception list are absent, and lockdown is absent or disabled. |
| Partial or conflicting | The settings do not form a supported ON or OFF configuration. |
| Unverified or unreachable | The app cannot establish the current result. A previous read is not confirmation of the present state. |

The app checks system settings, not live VPN traffic. An ON result does not prove that the tunnel is connected or that every traffic path has been tested.

For both actions, the app checks the target, writes the settings, reads them back and then requests a restart. After startup it checks the target and settings again. Failed writes or inconsistent readback prevent the automatic restart. Lost connections do not trigger repeated writes or restart requests.

If an operation is incomplete, restore the connection and tap **Read state again**. Check the new result before choosing another action. Closing or uninstalling the phone app does not undo settings already saved on the Chromecast.

## Target and preflight

The supported profile is **Chromecast with Google TV HD** (`boreal`), **Android 14/API 34**, Android user **0**, with Proton VPN (`ch.protonvpn.android`) installed and configured. Other TV models, firmware versions, users and VPN providers are not supported.

The phone stores its own pairing credentials and checks the saved TV identity on connections. Clearing app data, reinstalling the app or revoking its authorization may require pairing again. If app data must be cleared during recovery, check the TV's configuration first: clearing phone data also removes the record of any unfinished operation.

Use only a **trusted local network** and never expose ADB to the Internet. The ADB client does not verify the TV's TLS server certificate. Pairing and saved device checks help prevent accidental targeting but do not protect against an active attacker impersonating the TV on the network.

## USB recovery

This procedure removes **both** Android always-on VPN and the system kill switch. It leaves Proton's own Auto-connect preference unchanged.

You need a computer with Android SDK Platform Tools available as `adb`, a USB data connection with adequate power, **USB debugging enabled on the Chromecast**, and authorization for that computer. Prepare and check USB access before relying on it as a recovery route. Wireless ADB connectivity is not needed.

Run the commands in **Bash**. Connect only the intended USB device: `adb -d` selects USB and avoids a duplicate wireless connection. If several USB devices are attached, disconnect the others or replace every `-d` below with `-s VERIFIED_SERIAL`.

### 1. Check the connected device

```sh
adb devices -l
adb -d shell getprop ro.product.device
adb -d shell getprop ro.build.version.sdk
adb -d shell am get-current-user
adb -d shell pm path ch.protonvpn.android
adb -d shell settings --user 0 get secure always_on_vpn_app
adb -d shell settings --user 0 get secure always_on_vpn_lockdown
adb -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist
```

Confirm that the connected device is your Chromecast, with device name `boreal`, API `34`, user `0` and Proton installed. The VPN app setting should be `ch.protonvpn.android` or `null`, lockdown should be `1`, `0` or `null`, and the exception list should be `null` or empty. Stop if a command fails, authorization is missing, or the output identifies another device or VPN configuration.

### 2. Remove the system settings and restart

```bash
if adb -d shell settings --user 0 delete secure always_on_vpn_lockdown &&
   adb -d shell settings --user 0 delete secure always_on_vpn_app &&
   adb -d shell settings --user 0 delete secure always_on_vpn_lockdown_whitelist &&
   CC_VPN_APP=$(adb -d shell settings --user 0 get secure always_on_vpn_app) &&
   CC_VPN_LOCKDOWN=$(adb -d shell settings --user 0 get secure always_on_vpn_lockdown) &&
   CC_VPN_WHITELIST=$(adb -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist) &&
   [ "${CC_VPN_APP%$'\r'}" = "null" ] &&
   [ "${CC_VPN_LOCKDOWN%$'\r'}" = "null" ] &&
   [ "${CC_VPN_WHITELIST%$'\r'}" = "null" ]; then
    echo "System configuration removed. Restarting the Chromecast..."
    adb -d reboot
else
    echo "Removal not verified. Check the errors before continuing."
fi
```

`Deleted 0 rows` is normal when a setting was already absent. The restart is requested only after all three values read back as `null`. If the reboot command itself fails, check the USB connection and authorization before continuing.

### 3. Verify after startup

```sh
adb -d shell settings --user 0 get secure always_on_vpn_app
adb -d shell settings --user 0 get secure always_on_vpn_lockdown
adb -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist
```

After successful reads, OFF means the app value is `null`, lockdown is `null` or `0`, and the exception list is `null` or empty. A command error or missing output is not confirmation.

## Command reference

The app uses these Android secure settings for user `0`:

| Setting | ON | OFF before restart |
| --- | --- | --- |
| `always_on_vpn_app` | `ch.protonvpn.android` | Deleted |
| `always_on_vpn_lockdown` | `1` | Deleted |
| `always_on_vpn_lockdown_whitelist` | Absent or empty; existing exceptions block changes | Deleted |

Enabling writes the VPN app first and lockdown second. Disabling deletes lockdown, the VPN app and the exception list in that order. A normal OFF operation also handles always-on remaining enabled after lockdown was already removed.

For implementation details, see [OperationEngine.kt](../app/src/main/java/it/sorguido/chromecastkillswitch/core/OperationEngine.kt). Build and contributor instructions are in [BUILD.md](BUILD.md).
