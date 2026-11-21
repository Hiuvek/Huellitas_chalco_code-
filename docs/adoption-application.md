# HU-05 — Enviar una solicitud de adopción

Desde los requisitos del perrito se pulsa **Iniciar solicitud**. Si no hay sesión, se permite iniciar sesión o crear una cuenta y regresar al mismo perrito. El formulario tiene cinco pasos: contacto, hogar, otras mascotas, motivos y revisión. Las respuestas se conservan al regresar o editar una sección durante la sesión.

## Datos y validación

- Contacto: nombre de 2–100 caracteres, correo válido y teléfono de 10 dígitos.
- Hogar: tipo de vivienda, tenencia, espacio exterior, de 1 a 20 personas y horas que estaría solo.
- Otras mascotas: respuesta Sí/No obligatoria; descripción opcional de hasta 500 caracteres. Si la respuesta es No, se guarda `detalle=null`.
- Motivos: de 30 a 2000 caracteres, sin aceptar una respuesta formada solo por espacios.
- Se revisa todo antes de enviar. Los campos se validan también en las reglas del servidor.

La salud y los requisitos del perrito no se modifican al solicitar. El perrito debe estar publicado y Disponible, en un refugio activo de Chalco con requisitos y pasos configurados. El control privado no debe tener solicitud aprobada ni adopción. En proceso no permite iniciar otra solicitud.

## Guardado

| Documento | Datos |
| --- | --- |
| `solicitudes/{uuid}` | `folio`, `usuarioUid`, `perritoId`, `refugioId`, `estado`, `contacto`, `hogar`, `otrasMascotas`, `motivos`, `motivoNoAprobacion`, `creadoEn`, `actualizadoEn`. |
| `solicitudes_claves/{hash}` | `usuarioUid`, `perritoId`, `solicitudId`. |
| `historial_solicitudes/{uuid}-1` | `solicitudId`, `usuarioUid`, `refugioId`, `estadoAnterior`, `estadoNuevo`, `motivo`, `actorUid`, `creadoEn`. |

El folio es `HC-{uuid}`. La clave única conserva el modelo original: SHA-256 de UID, un separador NUL y el ID del perrito, expresada en hexadecimal minúsculo. Existe una solicitud por cuenta y perrito; un intento posterior consulta la existente sin reemplazar sus respuestas.

La ficha, clave e historial se crean en una transacción. El estado inicial es Enviada y las fechas proceden del servidor. Si falta uno de los documentos, las reglas rechazan el conjunto. El solicitante no puede modificar o borrar lo confirmado ni adelantar una aprobación.

Una comprobación explícita del servidor evita confirmar sin conexión. Mientras se envía, se bloquean otro envío y la edición. Un fallo conserva los mismos datos e identificador; el reintento comprueba el resultado anterior. En una carrera entre dos envíos, el cliente que recibe un rechazo consulta la clave y solo devuelve el folio si puede verificar en el servidor la solicitud del mismo propietario. No informa éxito a partir de un error aislado o de la caché.

## Cuentas y privacidad

Una cuenta nueva de Firebase Authentication puede solicitar sin un perfil administrativo en Firestore. Si existe un perfil desactivado, se bloquean sus operaciones privadas. La autorización de refugios conserva el requisito de perfil, refugio y membresía activos: crear una cuenta de adoptante no concede administración.

Las respuestas, el historial y la clave son privados. El solicitante consulta sus documentos y el responsable autorizado consulta únicamente los de su refugio; otra cuenta o refugio no los puede leer. La clave queda reservada al propietario. Los borradores de memoria se separan por UID y perrito; cerrar sesión retira el formulario y otra cuenta no recibe sus campos.

Todavía no se promete persistencia tras cerrar el proceso: el guardado con Room corresponde a HU-17. La consulta habitual de solicitudes se conecta en HU-06 y su evaluación en HU-12/HU-13.

## Entorno y recorrido

Se mantienen Spark y los emuladores de `demo-huellitas-chalco`. No se publican estas reglas en Firebase real. Arrancar y preparar el entorno siguiendo [dog-publication.md](dog-publication.md), y ejecutar `seed:local` y `seed:catalog`.

1. Abrir Lunita HU02 en el catálogo y consultar sus requisitos.
2. Pulsar Iniciar solicitud; crear una cuenta de prueba o iniciar sesión.
3. Completar los pasos, regresar para corregir un dato y revisar las respuestas.
4. Enviar y esperar la confirmación con folio y estado Enviada.
5. Repetir para el mismo perrito con la misma cuenta: debe aparecer la solicitud existente.

Las fichas `hu02-*` pertenecen al escenario de catálogo. Para revisar después la solicitud desde el refugio de prueba A, registrar un perrito desde esa cuenta y solicitarlo con una cuenta de adoptante; el responsable solo administrará su propio refugio.

## Pruebas

Verificación final: **75 pruebas unitarias totales, 73 de reglas y 19 Android** (8 SDK de envío, 3 de interfaz de solicitud y 8 de regresión), sin fallos. Compilación normal/local y Android Lint sin errores.

Se verifican datos obligatorios, límites, pasos, conservación de respuestas, doble envío, reintento, tiempo de espera, identidad tardía y separación de cuentas. Las pruebas de reglas comprueban integridad atómica, unicidad, estados, permisos y privacidad. El SDK Android comprueba envío real, dos intentos simultáneos, desconexión, reintento y lectura ajena. La interfaz recorre registro desde requisitos, retorno al perrito elegido, corrección de respuestas y confirmación, además de la separación de cuentas.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
npm --prefix tools/firebase run test:rules
```

Detener los servicios locales antes de las pruebas de reglas, reiniciarlos y preparar ambos escenarios antes de ejecutar `AdoptionApplicationSdkTest` y `ApplicationFlowTest` con los APK locales instalados.

Fuentes: [transacciones de Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions), [hashing en reglas](https://firebase.google.com/docs/reference/rules/rules.hashing) y [normalización de cadenas](https://firebase.google.com/docs/reference/rules/rules.String).
