# Build and contribute

Run commands from the repository root. The app can be built without the maintainer's signing key or any private files.

## Requirements

Use JDK 25, Android SDK platform 36 and Build Tools 36.0.0. The repository pins Gradle 9.6.0, Android Gradle Plugin 9.4.0 and Kotlin 2.2.10; dependency locks and verification hashes are included. Builds target Java 17 bytecode and Android API 26 or later.

Set `ANDROID_HOME` to your SDK, or set `sdk.dir` in `local.properties`. The build script also detects `development/local/sdk`. Initial dependency downloads need access to Google Maven, Maven Central, the Gradle Plugin Portal and JitPack.

Use the included Gradle wrapper. The build script uses `$HOME/.gradle` unless `GRADLE_USER_HOME` is set. Project caches and build outputs go under the ignored `development/` directory.

## Build an APK

```sh
./scripts/build.sh --unsigned
```

This runs unit tests, release lint and the release build. The APK is written to `development/build/app/outputs/apk/release/`. An unsigned APK must be signed before it can be installed.

To make an installable release build, create your own signing key and a private `development/signing.properties` file:

```properties
storeFile=development/signing/my-release.p12
storePassword=YOUR_PRIVATE_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_PRIVATE_KEY_PASSWORD
```

`storeFile` is relative to the repository root, or can be an absolute path. Keep the key and configuration out of Git, and retain a private backup. Then run:

```sh
./scripts/build.sh
```

The script requires the signing configuration; it does not fall back to a debug signature. An APK signed with a different key normally requires uninstalling the existing app first. Check the Chromecast's configuration before uninstalling: local pairing and pending-operation data are removed, and you will need to pair again.

## Test changes

The build script runs unit tests and lint. To build the instrumentation test packages:

```sh
./gradlew \
  --project-cache-dir development/gradle-project \
  :app:assembleDebug :app:assembleDebugAndroidTest
```

Install the APKs from `development/build/app/outputs/apk/debug/` and `development/build/app/outputs/apk/androidTest/debug/` on a dedicated emulator. Select its serial explicitly with `adb -s`, then run the `it.sorguido.chromecastkillswitch.test/androidx.test.runner.AndroidJUnitRunner` instrumentation runner.

Check pairing and operation-state handling as well as both languages, light/dark themes, large text and activity recreation. The real ADB pairing test is opt-in and restricted to emulator loopback. Emulator fixtures do not establish compatibility with a physical TV.

## Package a release

Use a new version and tag. Keep the application ID and signing identity for updates to existing installations, and increase `versionCode` for each release. Build from a clean checkout and verify the APK's version, certificate and checksum.

Create source archives from the tagged Git tree, so that local keys, caches and working files are excluded. For example, to package the existing `v1.1.0` source:

```sh
mkdir -p development/releases
git archive --format=tar.gz --prefix=chromecast-killswitch-1.1.0/ \
  --output=development/releases/chromecast-killswitch-1.1.0-source.tar.gz v1.1.0
```

Distribute the signed APK, checksums, matching application source, runtime dependency sources and required notices together. Preserve the wrapper, dependency locks, build scripts, vendored source and license files in the source package. See [third-party notices](../THIRD_PARTY_NOTICES.md) for component attribution and distribution terms.

The runtime source collector requires Python 3.11+ and network access:

```sh
python3 scripts/collect-runtime-sources.py
```

It checks that the runtime dependency lock matches `scripts/runtime-sources.json`, downloads the pinned sources and verifies their hashes. Source archives, licenses and checksums are written under `development/release-sources/`; package that directory as the runtime-source supplement. Review source identities and hashes when updating dependencies. After uploading a release, download the APK and verify its hash and signature against the local file.

For modifications to the LGPL SPAKE2 library, use the [source and relinking instructions](../vendor/spake2-source/README.md).
