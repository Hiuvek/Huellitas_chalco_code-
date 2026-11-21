# HU-08 — Registrar y publicar perritos

El responsable autorizado captura nombre, edad aproximada, tamaño, sexo, descripción y entre una y seis fotografías. Revisa la información antes de publicar. La confirmación aparece después de subir todas las imágenes y guardar la ficha y su control privado en una transacción de Firestore.

## Entorno elegido

El propietario eligió conservar el plan Spark y probar esta historia con emuladores sin costo. El código usa los SDK de Firebase Authentication, Cloud Firestore y Cloud Storage, conectados únicamente al proyecto ficticio `demo-huellitas-chalco` cuando se activa el modo local.

La instalación de pruebas tiene el paquete `com.example.huellitaschalco.local` y el nombre **Huellitas Chalco (local)**. La compilación normal conserva el paquete original y conecta al proyecto real. Las nuevas reglas de HU-08 no se han publicado en el proyecto real.

[Cloud Storage requiere Blaze con facturación](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024). La activación y comprobación de fotografías en la nube quedan pendientes mientras se conserve Spark. La app muestra un aviso si Storage no está configurado; no confirma una publicación simulada.

## Comportamiento

- Nombre de 1 a 60 caracteres; edad entera de 0 a 360 meses, con captura en meses o años.
- Tamaño Chico, Mediano o Grande; sexo Hembra o Macho; descripción de 40 a 2000 caracteres después de quitar espacios al inicio y final.
- Selector de imágenes del sistema, sin permiso de acceso a toda la galería. Se aceptan JPEG, PNG o WebP de hasta 20 MB de entrada.
- Copia temporal privada: corrige orientación, reduce a un máximo de 1600 píxeles por lado y convierte a JPEG de hasta 2 MB. La nueva copia no conserva metadatos GPS.
- Hasta seis fotografías; la primera es la portada. Las imágenes se pueden quitar antes de publicar.
- Formulario y fotografías conservados en el ViewModel durante la sesión y los cambios de configuración. La persistencia tras cerrar el proceso corresponde a HU-17 con Room.
- Vista previa con los datos capturados. Al iniciar la publicación se fijan los datos y el identificador; los reintentos usan ambos para evitar duplicar la ficha, incluso si una respuesta anterior se perdió.
- Carga secuencial con indicación de progreso. Si una fotografía falla no se publica la ficha; se conservan los archivos para reintentar. Una vez confirmada, se puede consultar desde Mis perritos o registrar otro.
- Una consulta mínima de los registros y sus fotografías permite comprobar el alta. La edición de perritos corresponde a HU-10; el catálogo público y su detalle a HU-02/HU-03.

## Datos y seguridad

Se conserva el [modelo original](https://gist.github.com/Hiuvek/2fa98a6526de2a673dcb61de7ff5c94e):

| Recurso | Datos |
| --- | --- |
| `perritos/{id}` | `refugioId`, `nombre`, `edadMeses`, `tamano`, `sexo`, `descripcion`, `vacunacion`, `desparasitacion`, `esterilizacion`, `estado`, `publicado`, `fotos`, `esDemostracion`, `creadoEn`, `actualizadoEn` |
| Cada elemento de `fotos` | `archivoId`, `ruta`, `orden` |
| `perritos_control/{id}` | `refugioId`, `solicitudAprobadaId`, `adopcionId` |
| Storage | `perritos/{refugioId}/{uid}/{perritoId}/{archivoId}.jpg` |

El ID es la clave del documento. `estado` inicia en Disponible, `publicado` en true y la salud en Sin información. La captura de salud se implementará en HU-09. Los enlaces de solicitud aprobada y adopción inician en null. Las fechas de creación y actualización las asigna el servidor. Los registros locales tienen `esDemostracion=true`.

Las reglas exigen perfil, refugio y membresía activos, validan los campos, límites y rutas de fotografías, y obligan a crear ficha y control juntos. No permiten editar, borrar ni adelantar una adopción. Storage limita rutas a la cuenta y refugio autorizados, acepta JPEG de hasta 2 MB y bloquea la modificación de imágenes de fichas ya publicadas. Los archivos de fichas públicas se pueden leer; listar todo el almacenamiento está bloqueado.

Firestore no puede comprobar la existencia de objetos de Storage desde sus reglas. La aplicación verifica la finalización de las cargas antes de guardar la ficha; un cliente modificado con membresía podría inventar una ruta propia. Una validación cruzada estricta requeriría un servicio de servidor adicional. Los archivos de una carga abandonada pueden quedar pendientes en Storage; los reintentos reutilizan sus rutas. La limpieza automática de estas cargas no forma parte de esta entrega. En el entorno actual son únicamente datos del emulador.

## Ejecutar en la tableta

Desde la raíz del repositorio:

```bash
npm --prefix tools/firebase ci
npm --prefix tools/firebase run start
```

En otra terminal:

```bash
npm --prefix tools/firebase run seed:local
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8180 tcp:8180
adb reverse tcp:9199 tcp:9199
./gradlew -PfirebaseAuthEmulator=true -PfirebaseAuthEmulatorHost=127.0.0.1 :app:installDebug
```

Abrir **Huellitas Chalco (local)** → Acceso para refugios. Cuenta ficticia: `hu07-a@example.test`; contraseña exclusiva del emulador: `PruebaHU07!`. El segundo refugio usa `hu07-b@example.test` y el adoptante `hu07-adopter@example.test`, con la misma contraseña local. Ninguna de estas cuentas modifica la cuenta personal del proyecto real.

Para un emulador Android, omitir `firebaseAuthEmulatorHost` y los comandos `adb reverse`; el host inicial es `10.0.2.2`. No se necesita `google-services.json` para compilar el modo local. Las reglas requieren ejecutar Auth, Firestore y Storage con el mismo proyecto demo.

Los servicios locales deben permanecer activos y la tableta conectada por USB mientras se utiliza ese reenvío. Por defecto, los datos del emulador se reinician al detenerlo. Para conservarlos, consultar las opciones de [importación y exportación de emuladores](https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data). No subir exportaciones a Git.

## Pruebas

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
npm --prefix tools/firebase run test:rules
```

La segunda orden inicia sus propios emuladores: detener previamente otra instancia que use los mismos puertos. Las pruebas de reglas se ejecutan en serie porque limpian el mismo proyecto local.

Con los tres emuladores activos y los datos ficticios cargados, las pruebas Android de HU-08 son `DogPublicationSdkTest` y `DogRegistrationFlowTest`. El recorrido de interfaz sustituye únicamente el resultado del selector por una imagen ficticia; el formulario, importador y SDK Firebase son reales.

- Validaciones y límites, campos conservados tras un error y envío único durante la carga.
- Reintentos con el mismo identificador, sin reemplazar una publicación existente.
- Carga parcial fallida sin ficha publicada; recuperación posterior.
- Acceso por membresía, aislamiento entre refugios, tipos y tamaños de archivos.
- Publicación atómica de ficha y control, y comprobación de datos y fotografía guardados.
- Falta de conexión sin confirmación ni escritura de ficha en cola.
- Registro, revisión, regreso al formulario, publicación y consulta desde Mis perritos.

El resultado final de las comprobaciones se registra en el checklist de la entrega.

## Revisión del 21 de noviembre de 2026

Pasaron las 30 pruebas unitarias del proyecto (8 nuevas de HU-08), las 38 de reglas (15 nuevas de HU-08) y las 5 del SDK Android de publicación. También pasaron las 20 comprobaciones de regresión de autenticación y autorización en la tableta. Compilación normal y Android Lint sin errores. Se corrigió el factor inicial de reducción de imágenes detectado en la tableta.

El recorrido `DogRegistrationFlowTest` pasó en la tableta Samsung SM-X620: validó los campos, agregó una imagen ficticia mediante el selector de prueba, revisó los datos, volvió a editar conservándolos, publicó, consultó Mis perritos y abrió el detalle guardado. Se reserva el espacio de las imágenes del listado para evitar que la carga cambie la posición de las tarjetas. La compilación local y Android Lint posteriores a ese ajuste también pasaron. HU-08 queda comprobada en emuladores; la activación de Storage y la prueba en el proyecto real permanecen pospuestas por la decisión de conservar Spark.
