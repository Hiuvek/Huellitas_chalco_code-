# HU-01 — Cuenta de adoptante

El registro y el acceso usan Firebase Authentication con correo y contraseña. Se guarda el nombre en el perfil de Firebase; el UID identifica la cuenta. Firebase mantiene la sesión entre aperturas. El perfil muestra el nombre y correo autenticados y permite cerrar la sesión.

Los errores de campos, correo duplicado, credenciales incorrectas y conexión se presentan en español. Los formularios conservan lo escrito al fallar, impiden envíos simultáneos y solo navegan al catálogo cuando termina la operación. La contraseña permanece en memoria del formulario y se borra al completar el acceso; la app no la guarda en Room, archivos ni estado persistente.

## Proyecto real

- Proyecto: `huellitas-chalco`, plan Spark.
- Paquete Android: `com.example.huellitaschalco`.
- Proveedor habilitado: correo electrónico/contraseña.
- Configuración local: `app/google-services.json`, excluido de Git.

Para otra computadora, descarga la configuración Android desde **Firebase → Configuración → General → Tus apps** y colócala en esa ruta. El archivo local de esta entrega contiene los identificadores y la clave Android verificados en las consolas de Firebase y Google Cloud; se recuperó así porque el navegador detenía la descarga. No contiene credenciales administrativas. Si falta, la interfaz informa que el acceso no está disponible y deshabilita el envío.

Compilar normalmente conecta con el proyecto real:

```bash
./gradlew :app:assembleDebug
```

La BoM de Firebase 34.6.0 mantiene compatibilidad con el compilador Kotlin 2.2 de este proyecto. Authentication usa el módulo principal `firebase-auth`.

## Arquitectura

| Carpeta | Responsabilidad |
| --- | --- |
| `feature/auth/domain` | Cuenta, contrato del repositorio, validación y errores |
| `feature/auth/data` | Operaciones y sesión del SDK Firebase |
| `feature/auth/presentation` | Estado del formulario, ViewModel y pantalla Compose |
| `core/firebase` | Inicialización del proyecto real o del emulador explícito |
| `app` | Contenedor de dependencias y conexión con la navegación |

## Pruebas locales con Firebase Auth Emulator

Desde la raíz del repositorio:

```bash
npm --prefix tools/firebase ci
npm --prefix tools/firebase run auth:start
./gradlew -PfirebaseAuthEmulator=true :app:connectedDebugAndroidTest
```

El emulador usa `demo-huellitas-chalco`, escucha en `127.0.0.1:9099` y ofrece su interfaz en `http://127.0.0.1:4000`. Sus cuentas no se envían al proyecto real. Para un dispositivo físico conectado por USB:

```bash
adb reverse tcp:9099 tcp:9099
./gradlew -PfirebaseAuthEmulator=true -PfirebaseAuthEmulatorHost=127.0.0.1 :app:connectedDebugAndroidTest
```

La configuración de red de debug permite HTTP únicamente a las direcciones locales del emulador. Release siempre deshabilita el emulador. Si necesitas aislar las cachés de herramientas, define `FIREBASE_EMULATORS_PATH` y `XDG_CONFIG_HOME` en carpetas de trabajo.

Si una instalación existente tiene otra firma, usa un dispositivo de prueba o una variante temporal con otro `applicationId`; no borres los datos del usuario para instalar las pruebas. En esta revisión se utilizó `.authpreview` mediante un script externo de Gradle y se omitió la tarea de Google Services solo en esa compilación temporal.

## Cobertura

- `AuthValidationTest`: datos obligatorios, formato del correo y longitud de contraseña.
- `AuthViewModelTest`: validación, envío único, espera del resultado, errores, conservación de campos y reintento.
- `FirebaseAuthRepositoryTest`: SDK Android contra el emulador; registro, nombre, acceso, cierre, sesión, correo duplicado y contraseña incorrecta.
- `AuthFlowTest`: recorrido Compose de registro, recreación de actividad, perfil, cierre, error de acceso y reintento.
- `CloudAuthSmokeTest`: prueba opcional contra un proyecto real; requiere `cloudSmoke=true` y los argumentos `firebaseProjectId`, `firebaseAppId` y `firebaseApiKey`. Se omite por defecto. Crea una cuenta ficticia `hu01-…@example.test`, verifica registro, nombre, acceso incorrecto, acceso correcto y recuperación de la sesión al reinicializar Firebase. La cuenta de revisión permanece identificada como `Prueba HU-01` y termina sin sesión en el dispositivo.

## Alcance

Esta historia no crea colecciones de Firestore, fotografías en Storage ni roles de refugio. HU-07 implementará la autorización y sus reglas de acceso. El catálogo, solicitudes, mapas y demás pantallas mantienen sus datos de demostración.

Referencia: [Firebase Authentication para Android](https://firebase.google.com/docs/auth/android/password-auth) y [conexión al emulador](https://firebase.google.com/docs/emulator-suite/connect_auth).

## Revisión del 20 de noviembre de 2026

- Compilación normal con configuración real y emulador desactivado: correcta.
- Pruebas unitarias: 14 nuevas de HU-01 y la prueba existente, sin fallos.
- Dispositivo Samsung SM-X620: 3 pruebas del repositorio contra el emulador y 1 recorrido de interfaz, sin fallos.
- Proyecto real `huellitas-chalco`: 1 prueba del ciclo de cuenta y sesión, sin fallos.
- Android Lint: 0 errores; 10 avisos sobre versiones de dependencias disponibles.
