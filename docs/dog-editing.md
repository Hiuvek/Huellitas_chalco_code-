# HU-10 — Revisar y editar mis perritos

Desde **Mis perritos**, el responsable abre una ficha guardada y pulsa **Editar perrito**. Se cargan sus datos y fotografías originales. Puede corregir nombre, edad (meses o años), tamaño, sexo y descripción, agregar o quitar fotografías y elegir la portada. Debe conservar entre una y seis imágenes y revisar los cambios antes de guardarlos.

La edición conserva el folio, refugio, fecha de creación, estado de adopción y control privado. La salud sigue administrándose por separado mediante HU-09. La lista propia y la ficha pública por identificador consultan el registro actualizado en Firestore. El catálogo general conserva los ejemplos hasta conectar HU-02; HU-10 no completa esa historia.

## Comprobar en la tableta

1. Iniciar los emuladores, preparar las cuentas e instalar **Huellitas Chalco (local)** siguiendo [dog-publication.md](dog-publication.md).
2. Iniciar sesión desde Acceso refugios con `hu07-a@example.test` y abrir uno de sus registros.
3. Pulsar **Editar perrito**. Corregir datos, agregar fotografías, seleccionar **Usar como portada** o **Quitar**. Revisar los cambios, volver a editar si hace falta y pulsar **Guardar cambios**.
4. Esperar **¡Cambios guardados!**, volver al registro y abrir **Ver ficha pública**. Comprobar los nuevos datos, fotografías y la salud conservada.
5. Editar sin guardar y pulsar **Cancelar edición**: se pide confirmar si hay cambios. Salir conserva la ficha guardada; continuar conserva el formulario.

## Guardado y errores

- El formulario usa las mismas validaciones del alta: nombre de 1–60 caracteres, edad entera de 0–360 meses, tamaño y sexo admitidos, descripción de 40–2000 caracteres y de una a seis fotos distintas por identificador.
- Las fotografías originales se reutilizan mediante sus referencias. Las nuevas pasan por el importador existente: JPEG, PNG o WebP de entrada de hasta 20 MB, salida JPEG de hasta 2 MB y lado máximo de 1600 píxeles, sin los metadatos originales.
- Se cargan primero las fotos nuevas y después se actualiza la ficha en una transacción. Una carga parcial no cambia los datos ni retira las referencias anteriores.
- La confirmación espera al servidor. Mientras guarda se bloquea otro envío. Después de un fallo se conservan y fijan los mismos datos y rutas para reintentar; la comprobación inicial reconoce un intento ya guardado y evita repetirlo o alterar su fecha.
- Si otro responsable cambió los datos generales o las fotos, se conserva el formulario y se ofrece **Cargar registro actual**, que descarta las selecciones y archivos temporales de esa edición antes de recargar. Los cambios de salud no se sobrescriben ni provocan ese conflicto, porque se guardan aparte.
- Sin conexión, la comprobación explícita del servidor falla antes de subir fotos o escribir. La transacción no usa una escritura persistente en cola. Un tiempo de espera se presenta como resultado sin confirmar; el reintento vuelve a comprobarlo.
- El borrador vive durante la sesión y los cambios de configuración; conservarlo tras cerrar el proceso corresponde a HU-17 con Room.

## Datos y reglas

| Recurso | Uso en HU-10 |
| --- | --- |
| `perritos/{id}` | Actualiza `nombre`, `edadMeses`, `tamano`, `sexo`, `descripcion`, `fotos` y `actualizadoEn` con fecha del servidor. |
| `fotos` | Conserva `archivoId`, `ruta`, `orden`; la primera referencia es la portada. |
| `perritos_control/{id}` | Se conserva sin cambios. |
| Storage | Las fotos nuevas se guardan en `perritos/{refugioId}/{uid}/{perritoId}/{archivoId}.jpg` con identificadores nuevos. |

Las reglas exigen perfil, refugio y membresía activos. Admiten referencias originales de otros responsables del mismo refugio y nuevas rutas de la cuenta actual; rechazan rutas ajenas, índices inválidos y duplicados. La actualización no puede cambiar propietario, creación, publicación, estado de adopción, indicador de demostración o campos ajenos. Salud y edición general se escriben por separado.

Storage permite preparar imágenes para un registro existente únicamente en su refugio. Las imágenes referenciadas por la ficha no se pueden sobrescribir ni borrar. Las nuevas o retiradas no son públicas mientras no estén referenciadas por una ficha publicada disponible o en proceso. Los responsables autorizados siguen pudiendo consultar archivos de su refugio.

Quitar una foto elimina su referencia, no borra el objeto remoto. Pueden quedar archivos de ediciones canceladas, cargas parciales o fotos retiradas; su limpieza automática queda pendiente. Firestore tampoco verifica por sí solo la existencia del objeto de Storage: el cliente espera la carga y una validación cruzada estricta requeriría un servicio adicional. En esta entrega todos esos archivos pertenecen a los emuladores.

## Entorno y pruebas

Se conserva Spark y se usa exclusivamente `demo-huellitas-chalco` en Authentication, Firestore y Storage Emulator. Las reglas nuevas no se han publicado en Firebase real. La instalación `.local` se mantiene separada de la app real.

Verificación completada: **46 pruebas unitarias, 55 de reglas y 35 Android**, además de compilación normal/local y Android Lint sin errores.

Las pruebas cubren validación, carga de datos, portada, descarte, reintentos, cargas parciales, lectura pública, desconexión, conflictos, conservación de salud y aislamiento entre refugios. Las pruebas de reglas también comprueban revocación y protección de imágenes referenciadas.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
npm --prefix tools/firebase run test:rules
```

Detener primero los emuladores locales para ejecutar las pruebas de reglas, pues estas inician sus propios servicios y reinician sus datos. Después reiniciar los tres emuladores y preparar las cuentas. Las pruebas Android usan `DogPublicationSdkTest` y `DogRegistrationFlowTest` con los APK locales instalados.

Fuentes: [campos permitidos por las reglas de Firestore](https://firebase.google.com/docs/firestore/security/rules-fields) y [condiciones de las reglas de Storage](https://firebase.google.com/docs/storage/security/rules-conditions).
