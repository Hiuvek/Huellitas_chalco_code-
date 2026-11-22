# Pruebas de ambos roles — Spark

Fecha: 22 de noviembre de 2026. Dispositivo físico: tableta Samsung conectada por USB. Pruebas automatizadas de pantallas Compose y SDK Android, con Auth/Firestore Emulator para escenarios aislados. Adicionalmente se verificó el proyecto real desde la app normal. No se utilizaron dos dispositivos físicos.

## Resultado

- 114 pruebas unitarias correctas.
- 85 pruebas de reglas de Firestore correctas.
- 63 comprobaciones de pantallas y SDK correctas en la tableta, contando cada escenario una sola vez.
- APK normal y local compilados. Android Lint correcto.
- Reglas e índices publicados en `huellitas-chalco`. Facturación desactivada al verificar el proyecto.

| Área | Escenarios correctos | Alcance |
| --- | ---: | --- |
| Flujos de ambos roles | 15 | Acceso del refugio, rechazo de adoptante en administración, registro con foto, catálogo y filtros, detalle y requisitos, solicitud, consulta privada, cierre y cambio de sesión, bandeja y evaluación |
| Entrega desde las pantallas | 1 | Fecha, carta firmada, confirmación y estado guardado |
| Agenda y seguimiento | 3 | Programación, resultado con fotografía, próxima revisión, cierre sin próxima revisión y reintento sin duplicación |
| Publicación y edición mediante SDK | 17 | Alta, reemplazo de fotografía, estados de salud, conflictos, funcionamiento sin conexión, permisos y recorrido completo hasta seguimiento |
| Privacidad, Room y asistente | 25 | 16 rutas protegidas, 7 pruebas de persistencia, catálogo/borrador sin conexión y asistente con error, descarte, edición y aceptación |
| Firestore real | 1 | El responsable publica una foto; otro cliente Firebase sin sesión y sin caché compartida consulta ficha e imagen por internet |
| Gemini real | 1 | App Check y generación de una propuesta; sin publicar perritos |

Los 15 flujos de la primera fila se validaron en un lote con 14 resultados correctos y un reintento final correcto de evaluación. No se contabilizan como correctos los intentos fallidos anteriores.

## Problemas encontrados y resueltos

1. **Acceso a la fotografía retirada.** La primera versión de las reglas permitía descargar una imagen antigua mientras su perrito siguiera público. Ahora la ruta debe coincidir con la foto actual del perrito. Se añadió una prueba que exige rechazar la referencia retirada y se publicaron las reglas corregidas.
2. **Cierre interrumpido de solicitudes.** La entrega y su control se confirman atómicamente; los cierres restantes se procesan con su historial en operaciones pequeñas. Se agregó «Completar cierre de solicitudes» para retomar los pendientes desde una entrega ya guardada, sin crear otra adopción. Se verificó el reintento en el ViewModel y los permisos de cierre en reglas.
3. **Pruebas antiguas incompatibles con Spark.** Se actualizaron expectativas de seis fotografías a una, transporte de operaciones y fotografías de fixtures. Los escenarios se restablecen entre pruebas que modifican las mismas solicitudes.
4. **Fechas y sincronización de pruebas.** Un seguimiento de prueba tenía fecha anterior a una entrega recién registrada: la app lo rechazó correctamente. La prueba toma ahora la fecha de la entrega. La evaluación esperaba ocultar un botón antes de recibir el nuevo estado del listener; ahora espera el estado final de la pantalla.

## Publicación y datos

La app normal instalada es `com.example.huellitaschalco`, versión 1.1, compilación debug con la firma original. Mantiene la sesión existente y funciona mediante internet sin `adb reverse`. El APK `.local` conserva los escenarios aislados y requiere emuladores.

El refugio `refugio-prueba-chalco`, marcado como demostración, recibió los requisitos y pasos escolares existentes en los fixtures. No se cambiaron sus demás datos. No se migraron las cuentas, perritos o solicitudes de emuladores a la nube.

La prueba real creó una ficha temporal y una imagen; ambas y su documento de control se retiraron al terminar. Gemini se probó con la instalación normal ya autorizada en App Check. Otra instalación debug puede necesitar su propia autorización de App Check para Gemini.

## Límites de la comprobación

- No se probó simultaneidad entre dos teléfonos físicos o dos redes móviles.
- La prueba real de Firestore cubre publicación y lectura con clientes independientes. El recorrido completo de adopción se verificó en la tableta contra emuladores con las mismas reglas publicadas.
- No se hicieron pruebas de carga ni de agotamiento de cuotas. Spark conserva sus límites gratuitos.
- No se verificó una distribución pública en Google Play ni Play Integrity en una versión release.

## Evidencia local

Los registros están fuera de Git en `/home/hiuvek/Documents/Codex/2026-11-16/ho/work/`:

- `spark-rules-final-retry.log`: 85 reglas correctas; el intento anterior tuvo un error transitorio del emulador al limpiar datos.
- `spark-normal-build.log`: compilación normal, pruebas unitarias y Lint.
- `spark-ui-roles-final.log` y `spark-ui-evaluation-final.log`: flujos y reintento de evaluación.
- `spark-ui-delivery.log`, `spark-ui-followups.log`, `spark-device-dogs-final.log` y `spark-device-privacy-offline.log`: escenarios complementarios.
- `spark-cloud-device.log`, `spark-normal-gemini.log` y `spark-cloud-deploy-final.log`: comprobaciones reales y publicación.
