# HU-07 — Acceso autorizado del refugio

El responsable inicia sesión con Firebase Authentication. La aplicación obtiene su perfil y sus membresías activas desde Cloud Firestore. Tener una cuenta no concede permisos de refugio; una cuenta sin membresías se trata como adoptante. Los datos conservan las colecciones del [modelo original](https://gist.github.com/Hiuvek/2fa98a6526de2a673dcb61de7ff5c94e).

## Datos de autorización

| Documento | Campos usados |
| --- | --- |
| `usuarios/{uid}` | `uid`, `nombre`, `correo`, `activo`; `creadoEn` es metadato del modelo |
| `refugios/{refugioId}` | `nombre`, `activo`, `zonaPublica`, `horarioPublico`; los otros campos públicos siguen el modelo original |
| `membresias/{uid}__{refugioId}` | `usuarioUid`, `refugioId`, `activo`; `creadoEn` es metadato del modelo |

Los dos guiones bajos separan UID e ID del refugio. El ID del documento y las referencias deben coincidir. Un administrador crea estos documentos desde Firebase Console o un entorno de servidor autorizado; la app no puede conceder, modificar ni reactivar sus membresías. No se guardan contraseñas en Firestore.

Para autorizar una cuenta existente: obtener su UID desde Authentication, crear su perfil activo, registrar el refugio activo y crear la membresía correspondiente. Para revocarla, cambiar `membresias/{uid}__{refugioId}.activo` a `false`. Desactivar el perfil o el refugio también retira los permisos. Una cuenta puede pertenecer a varios refugios y elegir entre ellos en la cabecera de la administración.

## Comportamiento de la aplicación

- Acceso para refugios conectado al mismo proveedor de correo y contraseña de HU-01.
- Verificación antes de mostrar cualquier destino administrativo, incluidos los abiertos desde el muestrario y los parámetros de debug.
- Perfil con la cuenta autenticada y los datos del refugio seleccionado; cierre de sesión real.
- Listener de membresías, perfil y refugios que retira el acceso al revocarse o cambiar de cuenta.
- Nueva comprobación al regresar a la app. Los permisos obtenidos de caché o con escrituras pendientes no conceden acceso; las consultas de autorización esperan al servidor.
- Los estados de espera o error mantienen bloqueada la administración y permiten reintentar o ir al catálogo público.
- Firestore utiliza caché en memoria; Room todavía está pendiente para el catálogo público y los borradores.

## Reglas del servidor

`firestore.rules` limita perfiles y membresías a su propietario, permite consultar datos públicos y restringe registros privados al refugio con membresía activa. Las solicitudes, su historial y las adopciones también pueden ser consultados por su adoptante propietario con perfil activo. Las consultas administrativas deben incluir `whereEqualTo("refugioId", idAutorizado)`; Firestore rechaza una consulta global de registros privados.

En las reglas de HU-07 publicadas en el proyecto real, las escrituras de negocio continúan bloqueadas. HU-08 incorpora el alta de perritos y su control en las reglas locales; consulta [dog-publication.md](dog-publication.md). Las demás escrituras se habilitarán con la validación y las transiciones de su historia (HU-05, HU-13, etc.). Así, esta entrega no habilita aprobaciones o adopciones sin validar su proceso. La creación de documentos mediante las pruebas se realiza solo con privilegios del emulador.

Configuración del proyecto: `firebase.json`, `firestore.rules` y `firestore.indexes.json`. El índice compuesto corresponde a `membresias(usuarioUid, activo)`. No se cambia a un plan de pago.

## Pruebas reproducibles

Desde la raíz:

```bash
npm --prefix tools/firebase ci
npm --prefix tools/firebase run test:rules
```

Las 23 pruebas de reglas comprueban visitante, adoptante, dos refugios, consultas con y sin filtro, membresías ajenas, intentos de autoautorización, revocación y referencias adulteradas. También verifican que las escrituras futuras permanezcan bloqueadas.

Para SDK Android y pantallas, iniciar Auth y Firestore y cargar datos ficticios:

```bash
npm --prefix tools/firebase run start
node tools/firebase/seed-local.mjs
```

En otra terminal, para la tableta por USB:

```bash
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8180 tcp:8180
./gradlew -PfirebaseAuthEmulator=true -PfirebaseAuthEmulatorHost=127.0.0.1 :app:connectedDebugAndroidTest
```

Para un emulador Android, omitir `firebaseAuthEmulatorHost`: el valor inicial es `10.0.2.2`. Auth escucha en `9099` y Firestore en `8180`; el puerto `8080` estaba ocupado en esta computadora. El proyecto de pruebas es únicamente `demo-huellitas-chalco`.

El cargador local crea `hu07-a@example.test`, `hu07-b@example.test` y `hu07-adopter@example.test`, con contraseña **solo para el emulador** `PruebaHU07!`. Las primeras cuentas pertenecen únicamente a su propio refugio. No son cuentas del proyecto real.

- `MembershipPolicyTest`: correspondencia de UID, ID, estado y sesión.
- `PrivateRoutesTest`: visitante y adoptante bloqueados en los 15 destinos privados.
- `ShelterGateTest`: revocación, cambio de cuenta, error y selector de refugios.
- `ShelterLoginFlowTest`: acceso real en la interfaz, rechazo del adoptante, cambio de cuenta y cierre de sesión.
- `FirestoreShelterAccessTest`: SDK Android, membresías de dos cuentas, aislamiento en servidor y bloqueo sin conexión.
- `CloudShelterAccessTest`: prueba opcional de solo lectura con una sesión existente del proyecto real; verifica la membresía y el rechazo de consultas a un refugio ajeno. Requiere `hu07CloudRole=true`, `expectedUid` y `expectedShelterId`; no crea cuentas ni solicita contraseñas.

Las pruebas H1 continúan vigentes. `CloudAuthSmokeTest` se omite por defecto para no crear cuentas reales automáticamente. Si hay una app instalada con otra firma, usar un dispositivo o variante temporal de prueba en lugar de borrar sus datos.

## Alcance

La autorización y las restricciones se conectan a Firebase. HU-08 conecta el registro y una consulta mínima de perritos a emuladores. Las variantes del muestrario, las solicitudes y los seguimientos continúan como ejemplos y muestran ese aviso. Su registro y gestión real pertenecen a las siguientes historias. Las reglas están publicadas y la membresía de la cuenta autorizada está activa en el proyecto real. El acceso y el aislamiento entre refugios están comprobados con la sesión autorizada en la tableta.

Referencias oficiales: [condiciones de seguridad](https://firebase.google.com/docs/firestore/security/rules-conditions), [pruebas de reglas](https://firebase.google.com/docs/rules/unit-tests) y [listeners del SDK](https://firebase.google.com/docs/firestore/query-data/listen).

## Revisión del 21 de noviembre de 2026

Compilación normal y Android Lint sin errores. Pasaron las 22 pruebas unitarias del proyecto (7 nuevas de autorización), las 23 de reglas y las pruebas de SDK e interfaz en Samsung SM-X620. El inicio de sesión del adoptante de HU-01 conserva sus pruebas de regresión. Con la autorización del propietario se publicaron las reglas, se habilitó el índice de membresías y se crearon el perfil, «Refugio de prueba Chalco» y su membresía activa. `CloudShelterAccessTest` pasó contra el proyecto real con la sesión autorizada: resolvió el refugio, leyó su membresía activa, permitió la consulta limitada a ese refugio y rechazó la de un refugio ajeno con `PERMISSION_DENIED`. No creó cuentas ni modificó datos durante la prueba.
