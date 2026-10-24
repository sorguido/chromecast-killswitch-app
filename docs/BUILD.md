# Build, signing and distribution

## Pinned toolchain

| Component | Version |
| --- | --- |
| Gradle wrapper | 9.6.0, pinned distribution SHA-256 |
| Android Gradle Plugin | 9.4.0 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| Android SDK | compile/target 36; minimum 26 |
| Build Tools | 36.0.0 |
| Java bytecode | 17 |
| Compose UI / Material 3 | 1.7.8 / 1.3.2 |
| Activity / Lifecycle | 1.10.1 / 2.8.7 |
| Coroutines | 1.10.2 |
| LibADB / SPAKE2 Android | 3.1.1 with documented patches / 2.2.1 |
| Conscrypt / Bouncy Castle | 2.5.3 / 1.81 |

Use a JDK supported by Gradle 9.6 (the recorded baseline used OpenJDK 25), Android SDK platform 36 and Build Tools 36.0.0. The wrapper, Gradle locks and dependency-verification metadata are included. Initial dependency downloads require network access to the repositories in `settings.gradle.kts`, including JitPack. No private signing key or personal workspace is needed to rebuild source.

Set `ANDROID_HOME` to your SDK, or configure `sdk.dir` in the local `local.properties` file. The build script also recognizes the project-local SDK under `development/local/sdk`. Install required SDK packages using your existing SDK tools; this project does not require modifying host-wide settings.

## Build without the release key

```sh
./scripts/build.sh --unsigned
```

This runs relevant unit tests, release lint and a release build without the original signing key. The APK is under `development/build/app/outputs/apk/release/`. An unsigned APK is not an installable signed release. The normal build uses the upstream locked SPAKE2 AAR; the [optional replacement recipe](../vendor/spake2-source/README.md) enables modification/relinking of that LGPL component.

## Sign your own build

Generate and keep your own signing key privately. Put `development/signing.properties` alongside the private key configuration:

```properties
storeFile=development/signing/my-release.p12
storePassword=YOUR_PRIVATE_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_PRIVATE_KEY_PASSWORD
```

Paths in `storeFile` are relative to the repository root (or may be absolute locally). Do not commit this file or print passwords in logs. Then run:

```sh
./scripts/build.sh
```

The script requires release signing configuration; there is no silent fallback to a debug signature. Preserve a private backup of your key and properties on media you control.

Android only accepts an in-place update when application ID and signing certificate match and the version code increases. A build signed with another key normally requires uninstalling the previous app first, losing pairing and pending local state. Check/recover the TV's configuration before clearing app data, then pair again. The maintainer's private signing key and phone ADB identity are not part of corresponding source.

## Emulator checks

Build the test packages using the project wrapper and project-local Gradle cache:

```sh
GRADLE_USER_HOME="$PWD/development/local/gradle" ./gradlew \
  --project-cache-dir development/gradle-project \
  :app:assembleDebug :app:assembleDebugAndroidTest
```

Install `development/build/app/outputs/apk/debug/app-debug.apk` and `development/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk` on a dedicated emulator, always selecting its serial explicitly with `adb -s`. Run `it.sorguido.chromecastkillswitch.test/androidx.test.runner.AndroidJUnitRunner`. Synthetic UI/transport fixtures exist only in test sources. Any real ADB pairing test is opt-in and restricted to emulator loopback; skipped pairing is not a physical PASS.

Locale checks include Italian/regional Italian, English, another primary language and French first with Italian second. Verify main screens, confirmations, failures and help in both languages, light/dark themes and large text, plus recreation without lost pairing or repeated mutations. Current results and limitations are in [PUBLICATION.md](PUBLICATION.md).

## Private layout and distribution

`development/` contains local signing material, caches, build outputs, raw evidence and working release packages. A small set of defensive ignore patterns prevents accidental secret/binary commits and covers tooling files that cannot reasonably be routed there. The ignored root `local.properties` is an SDK locator, not a required public input. Do not archive the working directory as release source.

A release must use a new tag on the verified commit, the original application ID/signing identity, and a higher version code. Do not overwrite existing tags/assets. Produce the source archive from the tracked tag, for example:

```sh
mkdir -p development/releases
git archive --format=tar.gz --prefix=chromecast-killswitch-1.1.0/ \
   --output=development/releases/chromecast-killswitch-1.1.0-source.tar.gz v1.1.0
```

Distribute the signed APK, `SHA256SUMS`, matching source, scripts and required license/notice material together. The source archive must include the Gradle wrapper/locks, `vendor/libadb`, `vendor/spake2-source`, `vendor/licenses`, `LICENSE`, `LICENSE-ADDITIONAL-PERMISSION` and `THIRD_PARTY_NOTICES.md`. Supply exact corresponding source for the linked runtime components as the release's dependency-source supplement; a generic upstream branch link is not a substitute. Keep all component attribution and required acknowledgments with binary distribution.

The checked-in [runtime source manifest](../scripts/runtime-sources.json) pins URLs and SHA-256 values against the release runtime dependency lock. Recreate the supplement without private inputs with Python 3.11+ and network access:

```sh
python3 scripts/collect-runtime-sources.py
```

The collector writes to `development/release-sources/`, validates archive structure and hashes, and includes the pinned Maven source/POM files, native Conscrypt/BoringSSL and Graphics Path sources, license material and an internal checksum list. Package that directory as the companion runtime-source asset. Updating the manifest is a separate maintainer action that requires reviewing the new source identities and hashes.

Build the tracked source from a clean checkout without private signing files, using an unsigned build or an explicitly separate test key. Verify application ID, version, certificate, APK contents and checksums. After publication, download the actual APK and compare hash/signature with the verified local file. Preserve repository visibility: publishing a release in a private repository does not make it public.
