# Product requirements

This document owns functional and interface requirements. [COMMAND_CONTRACT.md](COMMAND_CONTRACT.md) owns commands, state classification and operation sequencing.

## Scope

An Android phone controls one explicitly paired Chromecast over authorized wireless ADB. It enables or disables Android's always-on VPN and lockdown together, applying the change through a verified Chromecast restart. It is a local remote, not a VPN client or a continuous tunnel monitor.

| Item | Supported scope |
| --- | --- |
| TV | Chromecast with Google TV HD, device `boreal`, Android 14/API 34, Android user `0` |
| VPN | Proton VPN, `ch.protonvpn.android`, already installed and configured on the TV |
| Phone | Declared minimum Android 8/API 26; not restricted to a brand |
| Reported physical setup | Motorola edge 40 neo, Android 15; successful operational paths reported by the user, including from the app |
| Languages | Italian only when the first system language has language code `it`; English otherwise |
| Distribution | Complete signed release APK, matching source and notices; no demonstration behavior in release |

The reported phone does not prove all Android versions/ABIs. New-build verification and its limits are recorded separately in [PUBLICATION.md](PUBLICATION.md). Toolchain versions belong to [BUILD.md](BUILD.md).

## Functional requirements

| ID | Requirement and verifiable behavior |
| --- | --- |
| F01 | Control only the paired Chromecast and supported profile. Check saved identity, Android user and supported configuration before mutation. Another discovered device never automatically becomes the target. |
| F02 | Pair the phone with the code shown on the TV and persist its own ADB identity. A wrong code permits no mutation; PC credentials are unnecessary. |
| F03 | Discover current pairing/connection endpoints separately, deduplicate identical endpoints and resolve changed addresses/ports. An endpoint is not the saved identity. |
| F04 | Offer validated manual IP/port entry when discovery fails, with the same pairing and identity checks. |
| F05 | Read actual configuration and distinguish ON, OFF, partial, conflicting and unknown states. Failed reads, incomplete data or stale cache must not become a current ON/OFF claim. |
| F06 | After confirmation, enable both Android settings for Proton, read back, request restart and verify after startup. A successful write alone is insufficient. |
| F07 | After confirmation, remove all three settings in the contract, verify removal and restart. Disabling only lockdown while leaving always-on selected is not full OFF. |
| F08 | Distinguish settings saved, restart requested, startup awaited and configuration verified. A disconnected ADB session alone does not prove restart. |
| F09 | Stop the specified sequence on connection, authorization, write or readback failure. Explain what was verified and offer explicit recovery; do not restart after failed verification. |
| F10 | Preserve pairing and pending-operation meaning across recreation/reopening. Re-read and reconcile; never silently replay an uncertain mutation or restart. |
| F11 | Work locally in the foreground without phone root, an app account, cloud server, powered-on PC, permanent service, phone VPN service or additional TV APK. |
| F12 | Explain unreachable Wireless debugging and offer TV instructions, retry and manual endpoint entry. Restored connectivity must not replay failed mutations. |
| F13 | Provide offline localized help for pairing, local network, current endpoints, restart, TV-side debugging recovery and the conditional USB recovery route. |
| F14 | Implement real pairing, discovery, manual connection, reads, ON/OFF, restart/reconnection, errors and help in release. Synthetic states/transports belong only in tests. |
| F15 | Use complete Android EN-default/IT resource sets. Resolve messages in the current configuration, including domain/transport failures, progress and accessibility text. Only the first system language decides; regional Italian selects IT and secondary Italian does not. No manual/per-app language override, country/IP/SIM rule or global `Locale.setDefault` workaround. |

## Interface requirements

| ID | Requirement and verifiable behavior |
| --- | --- |
| U01 | A simple home screen identifies the paired TV, reachability, read configuration and two main operations. Pairing, technical details and help are secondary. |
| U02 | Main actions explicitly say “Enable and reboot” and “Disable and reboot”, with their Italian equivalents. Do not imply an immediate toggle. |
| U03 | Every enabled operation opens a mandatory confirmation; no “Don't show again”. No mutation or restart before confirmation. |
| U04 | Cancel performs no mutation. Prevent concurrent double taps or opposite operations. |
| U05 | Brief onboarding explains the TV pairing screen, target selection and code, without asking for Wireless debugging on the phone. |
| U06 | Explain saving, restart, waiting and verification with words rather than only colors. Offer pertinent recovery instead of a generic success message. |
| U07 | Unreachable-ADB guidance explains that the app cannot enable a disabled service; pairing credentials never appear in diagnostics. |
| U08 | Complete Material 3 light/dark themes follow system preference with readable spacing and hierarchy. |
| U09 | Touch targets are at least 48 dp; essential text/actions remain usable at 200% font size with scrolling as needed. Accessibility descriptions communicate control purpose and state; normal text contrast is at least 4.5:1. |
| U10 | Both languages preserve the safety distinction: configuration ON does not prove tunnel connection or absence of leaks; Proton Auto-connect is separate. Locale changes/recreation must not leave stale-language messages, lose pairing or replay operations. |

## Required confirmation content

The title matches the selected action. The concise text explains local reachability, active Wireless debugging, the required restart and interrupted playback. Disabled Wireless debugging must be re-enabled on the TV. OFF explicitly removes Android's always-on and lockdown while leaving Proton Auto-connect separate. Confirm triggers only the selected operation after fresh preflight checks; a recovery dialog grants no access to an offline device.

The owner's reported real tests are attributed evidence, not a newly observed acceptance campaign. Test only the risks affected by localization/publication; do not fabricate physical results or infer enforcement from the settings database.
