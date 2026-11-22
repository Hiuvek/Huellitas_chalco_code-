#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
args=(--max-workers=2 --no-configuration-cache -Pkotlin.compiler.execution.strategy=in-process -PfirebaseAuthEmulator=true -PfirebaseAuthEmulatorHost=127.0.0.1)
# Opcional: configuración local para conservar la firma de una instalación existente.
if [[ -n "${HUELLITAS_GRADLE_INIT:-}" ]]; then
  [[ -f "$HUELLITAS_GRADLE_INIT" ]] || { echo 'No existe el archivo de configuración de firma indicado.' >&2; exit 1; }
  args+=(--init-script "$HUELLITAS_GRADLE_INIT")
fi
./gradlew "${args[@]}" :app:assembleDebug
mkdir -p build/demo
cp app/build/outputs/apk/debug/app-debug.apk build/demo/Huellitas-Chalco-demo.apk
printf 'APK de demostración: %s/build/demo/Huellitas-Chalco-demo.apk\n' "$repo_root"
printf 'Es la instalación .local: necesita emuladores y conexión USB preparada.\n'
