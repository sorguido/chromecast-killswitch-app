# External evidence routing

These primary sources support the scoped claims below. Documentation explains protocols and APIs; it does not prove compatibility or a release's test results. Implementation and verification evidence belong to [COMMAND_CONTRACT.md](docs/COMMAND_CONTRACT.md), [BUILD.md](docs/BUILD.md) and [PUBLICATION.md](docs/PUBLICATION.md).

| ID | Claim and scope | Primary sources |
| --- | --- | --- |
| C1 | Wireless ADB uses separate pairing and TLS connection services; NSD resolves local endpoints | [AOSP ADB Wi-Fi](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/docs/dev/adb_wifi.md), [Android NSD](https://developer.android.com/develop/connectivity/wifi/use-nsd) |
| C2 | The integrated LibADB version exposes pairing and remote command APIs; its dependencies keep their own licenses | [LibADB 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), [local changes](vendor/libadb/LOCAL_CHANGES.md), [third-party notices](THIRD_PARTY_NOTICES.md) |
| C3 | Android always-on/lockdown settings and Proton Auto-connect are distinct | [Android VPN](https://developer.android.com/develop/connectivity/vpn), [AOSP VPN implementation](https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/connectivity/Vpn.java), [Proton Android TV](https://protonvpn.com/support/android-tv) |
| C4 | Android resources support localization; Compose resolves resources in the current configuration; platform per-app preferences are a separate feature | [Localization](https://developer.android.com/guide/topics/resources/localization), [Compose resources](https://developer.android.com/develop/ui/compose/resources), [App languages](https://developer.android.com/guide/topics/resources/app-languages) |
| C5 | GPLv3 text, third-party license compatibility and distribution duties must be considered for the actual composition | [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html), [Apache compatibility](https://www.apache.org/licenses/GPL-compatibility.html), [included GPL text](LICENSE), [actual component notices](THIRD_PARTY_NOTICES.md) |
| C6 | Rewriting branch/tag history does not establish deletion from cached commit views or other clones; GitHub determines support eligibility | [GitHub sensitive-data removal](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository), [git-filter-repo](https://github.com/newren/git-filter-repo) |

The first-system-language-only rule is a product requirement, not Android's ordinary best-match locale fallback. Source links to moving branches explain semantics and do not certify an exact device firmware. Actual results must carry their own revision and scope.
