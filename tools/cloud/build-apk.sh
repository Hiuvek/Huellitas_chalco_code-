#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
python3 tools/cloud/check-config.py
node tools/cloud/status.mjs --require-ready
args=(--max-workers=2 --no-configuration-cache -Pkotlin.compiler.execution.strategy=in-process -PfirebaseAuthEmulator=false)
if [[ -n "${HUELLITAS_GRADLE_INIT:-}" ]]; then
  [[ -f "$HUELLITAS_GRADLE_INIT" ]] || { echo 'No existe la configuración de firma indicada.' >&2; exit 1; }
  args+=(--init-script "$HUELLITAS_GRADLE_INIT")
fi
./gradlew "${args[@]}" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
mkdir -p build/cloud
cp app/build/outputs/apk/debug/app-debug.apk build/cloud/Huellitas-Chalco-Spark.apk
printf 'APK privado conectado a Firebase real: %s/build/cloud/Huellitas-Chalco-Spark.apk\n' "$repo_root"
printf 'No requiere computadora ni adb reverse para usar los servicios.\n'
