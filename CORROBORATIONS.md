# Technical references

Primary references for contributors working on the protocol, Android integration, localization and distribution. They explain platform behavior; compatibility claims must also match the app implementation and relevant tests.

| Topic | References |
| --- | --- |
| Wireless ADB pairing and connection | [AOSP ADB Wi-Fi](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/docs/dev/adb_wifi.md), [Android network service discovery](https://developer.android.com/develop/connectivity/wifi/use-nsd) |
| ADB client integration | [LibADB 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), [local modifications](vendor/libadb/LOCAL_CHANGES.md) |
| System VPN settings and Proton Auto-connect | [Android VPN](https://developer.android.com/develop/connectivity/vpn), [AOSP VPN implementation](https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/connectivity/Vpn.java), [Proton on Android TV](https://protonvpn.com/support/android-tv) |
| Language resources | [Android localization](https://developer.android.com/guide/topics/resources/localization), [Compose resources](https://developer.android.com/develop/ui/compose/resources) |
| Distribution and dependency licenses | [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html), [Apache compatibility](https://www.apache.org/licenses/GPL-compatibility.html), [component notices](THIRD_PARTY_NOTICES.md) |

The app deliberately follows the phone's first system language. That rule differs from Android's general best-match language fallback. References to moving upstream branches do not identify an exact Chromecast firmware revision.
