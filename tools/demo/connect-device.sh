#!/usr/bin/env bash
set -euo pipefail
adb_bin="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/platform-tools/adb"
if [[ ! -x "$adb_bin" ]]; then adb_bin="$(command -v adb)"; fi
if [[ $# -gt 1 ]]; then echo "Uso: $0 [serie-del-dispositivo]" >&2; exit 1; fi
if [[ $# -eq 1 ]]; then
  device="$1"
else
  mapfile -t devices < <("$adb_bin" devices | awk 'NR>1 && $2=="device" {print $1}')
  if [[ ${#devices[@]} -ne 1 ]]; then
    echo 'Conecta una tableta con depuración USB autorizada o indica su serie.' >&2; exit 1
  fi
  device="${devices[0]}"
fi
[[ "$("$adb_bin" -s "$device" get-state)" == device ]]
for port in 9099 8180 9199 5001; do "$adb_bin" -s "$device" reverse "tcp:$port" "tcp:$port"; done
printf 'Conexión USB preparada para %s. Mantén los emuladores encendidos en la computadora.\n' "$device"
