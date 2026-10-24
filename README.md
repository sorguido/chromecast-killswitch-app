# Chromecast Kill Switch

Control the system VPN kill switch on your **Chromecast with Google TV HD** from an Android phone. Enable or disable Android's always-on VPN and “block connections without VPN” settings together for **Proton VPN**.

The settings stay on the Chromecast when you close the app. Normal use needs no PC, root access or extra app on the TV.

<p align="center">
  <img src="docs/images/app-home-en.jpeg" width="340" alt="Chromecast Kill Switch home screen in English">
</p>

## Download

[Download the latest release](https://github.com/sorguido/chromecast-killswitch-app/releases/latest).

Choose the `.apk` file, open it on your **phone**, and allow installation from that source if Android asks. The release also includes checksums, source code and license notices.

## Requirements

- **Phone:** Android 8 or later. Not every phone or Android version has been tested.
- **TV:** Chromecast with Google TV **HD**, running **Android 14**, using its primary Android user. Other TV models and firmware versions are not supported.
- **VPN:** Proton VPN already installed, signed in and configured on the Chromecast.
- **Connection:** phone and Chromecast on the same trusted local network, with Wireless debugging enabled **on the TV**.

Use a trusted network and never expose ADB to the Internet. The ADB client does not verify the TV's TLS server certificate; pairing and saved device checks do not replace a trusted network.

## Set up once

1. On the Chromecast, open **Settings → System → Developer options → Wireless debugging**. If Developer options is hidden, open **Settings → System → About** and select **Android TV OS build** seven times, until the developer message appears. Menu labels may vary slightly.
2. Choose **Pair device with pairing code** on the TV and keep that screen open.
3. In the phone app, tap **Pair the Chromecast**. Select the discovered pairing service, or enter the IP address and pairing port shown on the TV, then enter its six-digit code.
4. Return to the TV's main Wireless debugging screen. In the app's **Connection and pairing** panel, select the connection service or enter the current connection address and port. **The connection port is different from the pairing port.**

The app remembers the paired Chromecast. If its address or port changes, discovery can find the new endpoint; manual entry is also available. Wireless debugging on the phone is not required.

## Enable or disable

| Action | Effect on the Chromecast |
| --- | --- |
| **Enable and reboot** | Enables Android's always-on VPN and kill switch for Proton VPN. |
| **Disable and reboot** | Removes both system settings and their exception list. Proton's own Auto-connect preference remains separate. |

Each action asks for confirmation, checks the saved settings, restarts the Chromecast and verifies the configuration after startup. The restart interrupts playback. If verification cannot finish, the app explains what still needs checking.

An ON status confirms the system configuration. It does not measure the VPN tunnel or test for traffic leaks. Disabling the system settings does not necessarily disconnect Proton VPN.

The interface follows the phone's first system language: **Italian** when it is Italian, **English** otherwise.

## Need help?

If the Chromecast cannot be reached, check Wireless debugging **on the TV**, then use **Read state again** or enter the current connection address and port. The app cannot turn a disabled debugging service back on remotely.

See [troubleshooting and USB recovery](docs/COMMAND_CONTRACT.md) for connection problems, incomplete operations and recovery from a computer.

## Source and license

To compile or contribute, see the [build instructions](docs/BUILD.md).

Copyright © 2026 sorguido. Original app code is licensed under [GPL-3.0-or-later](LICENSE), with a narrow [Conscrypt/BoringSSL linking permission](LICENSE-ADDITIONAL-PERMISSION). Dependencies retain their own [licenses and notices](THIRD_PARTY_NOTICES.md).

This independent project is not affiliated with Google, Motorola or Proton.
