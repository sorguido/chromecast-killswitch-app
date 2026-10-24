# SPAKE2 corresponding source

This directory contains two unmodified upstream source archives:

- [SPAKE2-Java, tag 2.2.1](https://github.com/MuntashirAkon/spake2-java/tree/2.2.1): `spake2-java-2.2.1.tar.gz`.
- [SPAKE2-C submodule, commit 0d15933e5ba3e662cb01245a7ac0dc9fca3eac31](https://github.com/MuntashirAkon/spake2-c/tree/0d15933e5ba3e662cb01245a7ac0dc9fca3eac31): `spake2-c-0d15933.tar.gz`. This is the Java tag's gitlink; GitHub's Java archive alone omits submodule source.

[SHA256SUMS](SHA256SUMS) verifies the archive bytes. Each archive preserves source attribution and original licenses. Readable [LGPL-3.0](COPYING.LESSER) and [GPL-3.0](COPYING) copies accompany distribution. Copyright 2021 Muntashir Al-Islam; derived portions retain their authors, including BoringSSL/Google code in the submodule and EdDSA/CC0 files in the Java variant.

The resolved tag is `2.2.1`; the upstream Gradle `version` variable still says `2.2.0`. Use the artifact coordinates, exact submodule revision and [AAR hash](../../THIRD_PARTY_NOTICES.md#sources-and-artifact-identity) to identify this version.

## Extract, modify and rebuild

`bash vendor/spake2-source/rebuild.sh --prepare-only` verifies checksums and extracts both archives under `development/local/spake2-rebuild`. The resulting working copy contains all source and may be edited. Running the script again does not overwrite a prepared copy.

Only the extracted copy's Gradle configuration is replaced: it uses this app's Gradle wrapper, AGP 9.4.0, Java 17 bytecode, SDK/Build Tools 36, minimum API 26, NDK `27.2.12479018`, CMake `3.22.1`, and maximum page alignment 16384. Cryptographic code remains in the extracted source. Install those SDK/NDK/CMake components in your own SDK and set `ANDROID_HOME` accordingly.

The normal app build uses the original JitPack AAR 2.2.1. Rebuilding the native library is optional and requires the tools listed above.

```bash
bash vendor/spake2-source/rebuild.sh
GRADLE_USER_HOME="$PWD/development/local/gradle" ./gradlew :app:assembleRelease \
  --project-cache-dir development/gradle-project \
  -Pspake2Aar="$PWD/development/local/spake2-rebuild/android/build/outputs/aar/android-release.aar" \
  --write-locks
```

The second command recombines the app with the modified library. `--write-locks` updates the local copy's dependency locks for that intentional replacement. Configure your own signing key following [BUILD.md](../../docs/BUILD.md). Original release private keys are unnecessary for installing a personally signed build: uninstall the differently signed previous app first, after checking/recovering its TV configuration. Uninstallation clears local pairing and pending state; pair again afterward. No Chromecast firmware modification is required.

A replacement preserves the Java/JNI interface expected by LibADB. A modified library needs its own tests and does not inherit the original release's qualification.

