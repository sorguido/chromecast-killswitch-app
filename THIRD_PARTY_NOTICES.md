# Third-party notices

Copyright © 2026 sorguido. The original application code is licensed under the GNU General Public License, version 3 **or any later version** (`GPL-3.0-or-later`), with the narrow [additional permission for Conscrypt/BoringSSL](LICENSE-ADDITIONAL-PERMISSION). The complete GPL text is in [LICENSE](LICENSE).

Third-party components retain their original copyright, licenses and disclaimers. Selecting Apache-2.0 for dual-licensed LibADB files does not relicense SPAKE2 or other dependencies. Original license texts below are preserved without translation.

## Components included in the APK

| Component/version | Upstream authors/copyright holders | License and included material |
| --- | --- | --- |
| LibADB Android 3.1.1, with local changes | Muntashir Al-Islam; authors named in individual file headers | Apache-2.0 selected for dual GPL-3.0-or-later / Apache-2.0 files; other BSD/MIT headers retained. [COPYING](vendor/libadb/COPYING), [Apache text](vendor/libadb/LICENSES/Apache-2.0), [local changes](vendor/libadb/LOCAL_CHANGES.md), complete module source. |
| SPAKE2 Android 2.2.1 | Copyright 2021 Muntashir Al-Islam | [LGPL-3.0](vendor/spake2-source/COPYING.LESSER), incorporating [GPL-3.0](vendor/spake2-source/COPYING); [exact source, submodule and rebuild recipe](vendor/spake2-source/README.md). |
| SPAKE2-C, commit `0d15933e5ba3e662cb01245a7ac0dc9fca3eac31` | Google Inc., Muntashir Al-Islam, Free Software Foundation and authors named in headers | Project LGPL-3.0; `spake2.c` retains its permissive ISC license; glibc-derived `sha512.c`/headers declare LGPL-2.1-or-later. Original texts and attribution remain in the included submodule archive. |
| Conscrypt Android 2.5.3 | The Android Open Source Project and Conscrypt contributors; Apache Harmony and Netty portions | [Apache-2.0](vendor/licenses/Conscrypt-LICENSE), [original NOTICE](vendor/licenses/Conscrypt-NOTICE). Its embedded BoringSSL portions have the separate terms below. |
| BoringSSL embedded by Conscrypt, commit `49f0329110a1d93a5febc2bceceedc655d995420` | OpenSSL Project, Eric Young, Google and individual authors | [Complete license](vendor/licenses/BoringSSL-LICENSE): original OpenSSL/SSLeay, ISC and MIT terms for the identified portions. Revision recorded from `org/conscrypt/conscrypt.properties` in the resolved AAR. |
| Bouncy Castle `bcprov`, `bcpkix`, `bcutil` jdk15to18, 1.81 | The Legion of the Bouncy Castle Inc. | [Bouncy Castle license](vendor/licenses/BouncyCastle-LICENSE), preserved from the resolved `bcprov` artifact. |
| AndroidX, Compose and Material 3 | The Android Open Source Project and AndroidX contributors | Apache-2.0 according to the resolved POMs; [complete text](vendor/libadb/LICENSES/Apache-2.0). Every module version is recorded in the [app lock](app/gradle.lockfile). Main versions: Activity 1.10.1, Lifecycle 2.8.7, Compose UI 1.7.8, Material 3 1.3.2. |
| Kotlin stdlib 2.2.10, kotlinx-coroutines 1.10.2, JetBrains annotations 23.0.0 | JetBrains and contributors | Apache-2.0 according to resolved POMs; [complete text](vendor/libadb/LICENSES/Apache-2.0). |
| Guava ListenableFuture 1.0 | Google and Guava contributors | Apache-2.0; [upstream](https://github.com/google/guava), [complete text](vendor/libadb/LICENSES/Apache-2.0). |

The complete SPAKE2-Java archive also preserves the unused Java variant with EdDSA-Java/CC0 attribution. Including its source does not mean every variant is linked into the APK. JUnit, Espresso and other test/build tools are separate from the release runtime; dependency locks distinguish configurations.

## Required acknowledgments

This product includes software developed by the OpenSSL Project for use in the OpenSSL Toolkit (http://www.openssl.org/).

This product includes cryptographic software written by Eric Young (eay@cryptsoft.com). This product includes software written by Tim Hudson (tjh@cryptsoft.com).

These are upstream attribution statements, not endorsements of this app. The full conditions and disclaimers accompany them in [BoringSSL-LICENSE](vendor/licenses/BoringSSL-LICENSE).

## Distribution and modification

The original OpenSSL/SSLeay conditions in this pinned BoringSSL version require separate consideration from Apache/GPLv3 compatibility. The original-code copyright holder grants only the explicit [section 7 linking permission](LICENSE-ADDITIONAL-PERMISSION) for the Conscrypt/BoringSSL combination; this does not claim the upstream licenses themselves have become GPL-compatible or been changed. See the GNU discussion of [GPL-incompatible libraries](https://www.gnu.org/licenses/gpl-faq.html#GPLIncompatibleLibs).

SPAKE2 remains an LGPL library. I accompany the APK with its notice, GPL/LGPL texts, exact source archives, application source and scripts needed to recombine the app with a modified library. I impose no restriction on modifying LGPL portions or reverse engineering necessary to debug such modifications. The app does not require a particular application signature to block a replacement library.

The concrete procedure is in [vendor/spake2-source/README.md](vendor/spake2-source/README.md). `-Pspake2Aar=/path/to/library.aar` intentionally replaces the Maven library in the LibADB module; without it, the build uses upstream AAR 2.2.1. Update locks in the modified source copy. A personal signing key allows installation after uninstalling a differently signed app, which requires pairing again. Check the TV configuration and recover any pending operation before clearing local data. Original signing/ADB private keys are not distributable source.

When conveying an APK, also provide these notices and complete corresponding source for the same revision, including `vendor/libadb`, `vendor/spake2-source`, `vendor/licenses`, build scripts, and the exact linked dependency-source supplement supplied with the release. Preserve all copyright/license texts and checksums; a generic link to `master` is insufficient. Retain permissive-license acknowledgments in binary distributions as well.

The alternate SPAKE2 recipe's syntax and source extraction were historically checked; that is not a claim that a rebuilt native library passed the upstream binary's tests. Current build/verification results belong to the [publication record](docs/PUBLICATION.md).

## Sources and artifact identity

[LibADB 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), [SPAKE2-Java 2.2.1](https://github.com/MuntashirAkon/spake2-java/tree/2.2.1), [SPAKE2-C exact revision](https://github.com/MuntashirAkon/spake2-c/tree/0d15933e5ba3e662cb01245a7ac0dc9fca3eac31), [Conscrypt 2.5.3](https://github.com/google/conscrypt/tree/2.5.3), and [BoringSSL license at the embedded revision](https://boringssl.googlesource.com/boringssl/+/49f0329110a1d93a5febc2bceceedc655d995420/LICENSE) support the recorded provenance. Resolved POMs and artifact hashes supplement these sources for transitive dependencies. Initial provenance inspection: 24 October 2026.

| Resolved AAR | SHA-256 |
| --- | --- |
| `spake2-android-2.2.1.aar` | `8798fb6c04b5d53a6307ed9481e9afe563227abd4e8b9e917c8514fa850071bb` |
| `conscrypt-android-2.5.3.aar` | `551ae4e301c571760d1791e647db6ed1dcb10d34dcae7aa12b67f220f2ce98d1` |
