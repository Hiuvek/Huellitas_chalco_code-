#!/usr/bin/env python3
"""Valida la configuración local, no la existencia de los servicios remotos."""
import json
import os
from pathlib import Path

root = Path(__file__).resolve().parents[2]
for name in ("FIRESTORE_EMULATOR_HOST", "FIREBASE_AUTH_EMULATOR_HOST", "FIREBASE_STORAGE_EMULATOR_HOST", "FUNCTIONS_EMULATOR"):
    if os.environ.get(name):
        raise SystemExit(f"Quita {name} antes de preparar el despliegue real.")
path = root / "app/google-services.json"
if not path.is_file():
    raise SystemExit("Falta app/google-services.json del proyecto real.")
config = json.loads(path.read_text())
info = config.get("project_info", {})
if info.get("project_id") != "huellitas-chalco":
    raise SystemExit("El proyecto debe ser huellitas-chalco.")
clients = config.get("client", [])
if not any(c.get("client_info", {}).get("android_client_info", {}).get("package_name") == "com.example.huellitaschalco" for c in clients):
    raise SystemExit("Falta la app Android com.example.huellitaschalco.")
print("Configuración local: huellitas-chalco, Android; Spark no requiere Storage ni Functions. No se muestran claves.")
