# HU-09 — Estado de salud

Vacunación, desparasitación y esterilización se capturan por separado como **Sí**, **No** o **Sin información**. El valor inicial es Sin información: la ausencia de datos no significa que el tratamiento no se haya realizado.

## Uso

1. Abrir **Huellitas Chalco (local)** e ingresar desde Acceso refugios con una cuenta de prueba autorizada.
2. Al registrar un perrito, seleccionar los tres valores de salud y comprobarlos en la revisión antes de publicar.
3. En **Mis perritos**, abrir el registro, cambiar los selectores y pulsar **Guardar salud**.
4. Esperar **Salud guardada correctamente** y abrir **Ver ficha pública**. Deben aparecer los valores guardados.

El catálogo todavía muestra ejemplos (HU-02). La ficha pública por identificador ya consulta el registro y su salud; los requisitos y la ficha completa del refugio corresponden a HU-03/HU-04. La edición de nombre, edad, descripción y fotos corresponde a HU-10.

## Datos y permisos

| Documento | Campos de HU-09 | Valores |
| --- | --- | --- |
| `perritos/{id}` | `vacunacion`, `desparasitacion`, `esterilizacion` | `Sí`, `No`, `Sin información` |
| `perritos/{id}` | `actualizadoEn` | Fecha asignada por el servidor |

No se crean colecciones adicionales. El alta guarda la salud junto con la ficha. Para actualizarla, el repositorio ejecuta una transacción y las reglas exigen un perfil, refugio y membresía activos. Solo permite cambiar los tres estados y la fecha; impide cambiar propietarios u otros campos. Los visitantes pueden consultar la salud de fichas publicadas disponibles o en proceso, sin iniciar sesión.

La confirmación espera al servidor. Mientras guarda no permite otro envío ni cambiar selecciones. Un fallo conserva el formulario para reintentar; sin conexión la transacción no deja una escritura pendiente. Repetir un guardado ya confirmado no modifica la fecha. Si otro responsable cambió la salud desde que se abrió el formulario, se conservan las selecciones y se solicita **Cargar estado actual** antes de guardar de nuevo.

## Entorno

Por decisión del propietario, se utilizan Authentication, Firestore y Storage Emulator con `demo-huellitas-chalco`. La instalación `.local` se mantiene separada de la app real. La nube continúa en Spark y **estas reglas no se han publicado en el proyecto real**. Arranque, cuentas ficticias y compilación local: [dog-publication.md](dog-publication.md).

## Verificación

- Pruebas unitarias: valores iniciales, espera y doble envío, errores, reintento, conflicto, recarga, actualización concurrente y tiempo de espera.
- Reglas: las 27 combinaciones al registrar; lectura pública; aislamiento de refugios; rechazo de estados inválidos, fechas falsas, cambios ajenos y permisos revocados.
- Android con SDK real contra emuladores: persistencia, lectura sin sesión, reintento idempotente, conflicto, falta de conexión y otro refugio.
- Interfaz: registro con salud, revisión, publicación, edición y consulta pública.

Fuentes: [restricción de campos en reglas](https://firebase.google.com/docs/firestore/security/rules-fields) y [transacciones de Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions).

Comprobación automatizada (desde la raíz del repositorio):

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
npm --prefix tools/firebase run test:rules
```

La prueba de reglas inicia sus propios emuladores de Firestore/Storage: detener primero los servicios locales, ejecutarla y después reiniciar y preparar las cuentas con `npm --prefix tools/firebase run start` y `npm --prefix tools/firebase run seed:local`. La prueba Android requiere instalar el APK local y el APK de pruebas; se ejecutan `DogPublicationSdkTest` y `DogRegistrationFlowTest` con el runner de la instalación `.local.test`.
