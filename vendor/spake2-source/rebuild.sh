#!/usr/bin/env bash
set -euo pipefail

# Estrae sorgenti immutati e crea soltanto una configurazione di build locale.
# Le dipendenze dell'app normale non cambiano.
SOURCE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd -- "$SOURCE_DIR/../.." && pwd)"
REBUILD_DIR="${SPAKE2_REBUILD_DIR:-$REPO_DIR/.local/spake2-rebuild}"
if [ -e "$REBUILD_DIR/settings.gradle.kts" ]; then
    printf '%s\n' "Uso la copia di lavoro esistente: $REBUILD_DIR"
else
    (cd "$SOURCE_DIR" && sha256sum -c SHA256SUMS)
    mkdir -p "$REBUILD_DIR"
    tar -xzf "$SOURCE_DIR/spake2-java-2.2.1.tar.gz" --strip-components=1 -C "$REBUILD_DIR"
    tar -xzf "$SOURCE_DIR/spake2-c-0d15933.tar.gz" --strip-components=1 -C "$REBUILD_DIR/android/src/main/cpp/spake2-c"
    # I file originali restano negli archivi; questa configurazione usa la toolchain dell'app.
    mv "$REBUILD_DIR/settings.gradle" "$REBUILD_DIR/settings.gradle.upstream"
    mv "$REBUILD_DIR/build.gradle" "$REBUILD_DIR/build.gradle.upstream"
    mv "$REBUILD_DIR/android/build.gradle" "$REBUILD_DIR/android/build.gradle.upstream"
    cat > "$REBUILD_DIR/settings.gradle.kts" <<'SETTINGS'
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "Spake2Rebuild"
include(":android")
SETTINGS
    cat > "$REBUILD_DIR/build.gradle.kts" <<'BUILD'
plugins { id("com.android.library") version "9.4.0" apply false }
BUILD
    cat > "$REBUILD_DIR/android/build.gradle.kts" <<'ANDROID'
plugins { id("com.android.library") }
android {
    namespace = "io.github.muntashirakon.crypto.spake2"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "27.2.12479018"
    defaultConfig {
        minSdk = 26
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies { implementation("androidx.annotation:annotation:1.9.1") }
ANDROID
fi
if [ "${1:-}" = "--prepare-only" ]; then
    printf '%s\n' "Sorgenti pronti in $REBUILD_DIR; nessuna build eseguita."
    exit 0
fi
: "${ANDROID_HOME:?Imposta ANDROID_HOME al tuo SDK Android con NDK 27.2.12479018 e CMake 3.22.1.}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$REPO_DIR/.local/gradle}"
"$REPO_DIR/gradlew" -p "$REBUILD_DIR" :android:assembleRelease
printf '%s\n' "AAR prodotto: $REBUILD_DIR/android/build/outputs/aar/android-release.aar"
