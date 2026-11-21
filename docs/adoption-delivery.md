# HU-14 — Registrar la entrega de la adopción

Desde una solicitud Aprobada se abre **Registrar entrega**. Se exige fecha real (AAAA-MM-DD, no futura), confirmación de entrega y carta firmada, y una confirmación final con solicitante, folio y fecha. No se muestra éxito hasta recibir la confirmación del servidor o consultar la entrega guardada.

## Operación del servidor

La función callable `recordAdoption` comprueba la identidad autenticada, perfil, refugio y membresía activos. Dentro de una transacción consulta solicitud, perrito, control y todas las solicitudes del mismo refugio y perrito. Exige una sola aprobación, perrito En proceso y ausencia de adopción anterior.

Se crea `adopciones/{perritoId}` con los campos del modelo original: solicitudId, perritoId, usuarioUid, refugioId, fechaEntrega, cartaFirmada, registradoPorUid y creadoEn. La fecha de entrega es Timestamp a medianoche de Chalco; creadoEn procede del servidor. El control conserva la aprobación y recibe adopcionId; el perrito pasa a Adoptado.

Todas las otras solicitudes Enviada o En revisión pasan a No aprobada con el motivo «El perrito fue adoptado por otra persona.» y su historial. Las decisiones anteriores y las solicitudes de otros perritos se conservan. Esta consulta y el cierre se hacen en el servidor para no depender de una lista de IDs enviada por el teléfono.

El ID del perrito permite una sola adopción. Reintentar la misma entrega por el mismo responsable conserva registro y fechas; otro intento incompatible se rechaza. Las escrituras directas de adopciones desde el cliente permanecen bloqueadas. El volumen escolar admite hasta 200 solicitudes pendientes por entrega, dentro del límite de una transacción; si se supera no se guarda ningún cambio.

El perrito Adoptado sale del catálogo público. Su adoptante puede seguir consultando su nombre desde Mis solicitudes mediante el registro de entrega, sin hacer pública la ficha adoptada. Las fotografías adoptadas mantienen sus permisos previos; esta entrega no amplía su lectura pública.

## Emuladores sin facturación

Se mantiene Spark. Se añade Functions Emulator en el puerto **5001**, junto a Auth 9099, Firestore 8180 y Storage 9199. No se despliegan funciones ni reglas al proyecto real. La futura publicación del backend se revisará al decidir la configuración de nube.

```bash
npm --prefix functions ci
npm --prefix tools/firebase ci
npm --prefix tools/firebase run start
```

En otra terminal, preparar seed:local, seed:catalog y seed:requests. Para la tableta conectada por USB añadir:

```bash
adb reverse tcp:5001 tcp:5001
```

El APK local usa el mismo proyecto demo y el protocolo callable oficial contra ese puerto, con el token de Firebase Authentication. Esto evita que el SDK de Functions intente solicitar un token FCM fuera de los emuladores; la configuración normal conserva el SDK de Functions. Las otras conexiones por USB siguen siendo necesarias.

## Pruebas

Las pruebas del backend se ejecutan solo con Auth, Firestore y Functions Emulator preparados; verifican cierre completo, fechas, carta, roles, revocación, reintentos y concurrencia:

```bash
FIRESTORE_EMULATOR_HOST=127.0.0.1:8180 npm --prefix functions test
```

Las pruebas de reglas comprueban que el registro no se pueda escribir directamente y que el acceso a la ficha adoptada siga siendo privado. Android comprueba la llamada autenticada al emulador y el recorrido desde la solicitud aprobada hasta la entrega confirmada. HU-15 añade la programación de revisiones.

Documentación de Firebase: [Functions Emulator](https://firebase.google.com/docs/emulator-suite/connect_functions) y [ejecutar funciones localmente](https://firebase.google.com/docs/functions/local-emulator).

Referencia del transporte local: [protocolo callable](https://firebase.google.com/docs/functions/callable-reference).

Verificación final: **89 pruebas unitarias totales, 85 de reglas, 6 del backend y 35 Android**, sin fallos; compilación normal/local y Android Lint sin errores.
