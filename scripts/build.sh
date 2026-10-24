#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build_options=()
if [[ "${1:-}" == --unsigned ]]; then
  build_options+=(-Punsigned)
  shift
elif [[ ! -f development/signing.properties ]]; then
  echo "Missing development/signing.properties; see docs/BUILD.md or use --unsigned." >&2
  exit 2
fi
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/development/local/gradle}"
if [[ -z "${ANDROID_HOME:-}" && -d development/local/sdk ]]; then
  export ANDROID_HOME="$PWD/development/local/sdk"
fi
./gradlew --project-cache-dir "$PWD/development/gradle-project" --max-workers=2 --console=plain "${build_options[@]}" :app:testDebugUnitTest :app:lintRelease :app:assembleRelease "$@"
