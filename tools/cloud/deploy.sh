#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
python3 tools/cloud/check-config.py
node tools/cloud/status.mjs
firebase_bin="$repo_root/tools/firebase/node_modules/.bin/firebase"
[[ -x "$firebase_bin" ]] || { echo 'Instala las dependencias de tools/firebase con npm ci.' >&2; exit 1; }
# Destino explícito; no importa datos de emuladores ni elimina funciones ajenas.
"$firebase_bin" deploy --project huellitas-chalco --config firebase.json \
  --only firestore:rules,firestore:indexes
