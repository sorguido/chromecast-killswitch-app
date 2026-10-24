#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f signing.properties ]]; then
  echo "Manca signing.properties: configura una firma release privata come descritto nel README." >&2
  exit 2
fi
export GRADLE_USER_HOME="$PWD/.local/gradle"
./gradlew --max-workers=2 --console=plain :app:testDebugUnitTest :app:lintRelease :app:assembleRelease "$@"
