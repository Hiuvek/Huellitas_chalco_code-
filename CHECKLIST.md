# Checklist de funcionalidades — Huellitas Chalco

Revisión del 21 de noviembre de 2026. Referencia: las 18 historias del Excel corregido, conservando sus identificadores.

HU-01 usa Firebase Authentication para registrar cuentas e iniciar y cerrar sesión. HU-07 usa membresías activas de Cloud Firestore para autorizar al responsable y limitarlo a sus refugios; su acceso y el rechazo de consultas a un refugio ajeno están comprobados con una sesión del proyecto real. HU-08 tiene registro, fotografías, publicación y consulta comprobados con emuladores en la tableta. HU-09 incorpora captura y actualización de salud con lectura en la ficha pública por identificador, también comprobadas en emuladores. HU-10 permite editar datos generales y fotografías con revisión, guardado y consulta del registro actualizado, comprobados en emuladores. Se mantiene Spark por elección del propietario. HU-02 conecta el catálogo público de Chalco, búsqueda, filtros y navegación por identificador, comprobados en emuladores. HU-03 completa la ficha pública con galería, características, salud e información del refugio. Las otras 11 historias siguen pendientes.

## Lo que ya está implementado

- [x] Pantallas para adoptantes y responsables del refugio.
- [x] Navegación de presentación entre pantallas, con regreso y barras inferiores.
- [x] Componentes comunes: campos, botones, tarjetas, indicadores y diálogos.
- [x] Tema visual y fotografías locales basados en los mockups.
- [x] Muestrario de 60 pantallas y variantes, accesible mediante el icono del ojo.
- [x] Vistas de ejemplo de errores, listas vacías, falta de conexión y confirmaciones.
- [x] Archivos organizados por pantallas, componentes, navegación, datos de ejemplo y tema.
- [x] Compilación del APK y Android Lint sin errores.

## Estado por historia

| ID | Historia resumida | Interfaz actual | Funcionalidad real |
| --- | --- | --- | --- |
| HU-01 | Crear cuenta e iniciar sesión | Formularios conectados a Firebase | Implementada y comprobada |
| HU-02 | Consultar catálogo de perritos en Chalco | Consulta pública de Firebase, fotografías, refugio, búsqueda y filtros reales | Implementada y comprobada en emuladores; nube pospuesta |
| HU-03 | Consultar detalle del perrito | Galería, características, salud y refugio del registro seleccionado desde Firebase | Implementada y comprobada en emuladores; nube pospuesta |
| HU-04 | Consultar requisitos y pasos | Requisitos de ejemplo y acceso al formulario | Pendiente |
| HU-05 | Enviar solicitud de adopción | Cinco pasos, revisión y confirmación de ejemplo | Pendiente |
| HU-06 | Consultar mis solicitudes | Lista, detalle y estados de ejemplo | Pendiente |
| HU-07 | Acceso autorizado del refugio | Inicio de sesión real, verificación de membresías y bloqueo central | Implementada y comprobada |
| HU-08 | Registrar y publicar perritos | Formulario, fotos reales, revisión y consulta conectados a Firebase local | Implementada y comprobada en emuladores; nube pospuesta |
| HU-09 | Registrar estado de salud | Captura, edición, guardado y lectura pública de los tres estados | Implementada y comprobada en emuladores; nube pospuesta |
| HU-10 | Revisar y editar mis perritos | Datos y fotos guardados, edición, portada y revisión conectados a Firebase local | Implementada y comprobada en emuladores; nube pospuesta |
| HU-11 | Redactar descripciones con Gemini | Propuesta fija, aceptación, descarte y error de ejemplo | Pendiente |
| HU-12 | Consultar solicitudes recibidas | Lista, respuestas y vista vacía de ejemplo | Pendiente |
| HU-13 | Evaluar solicitudes | Revisión, decisión, confirmación y bloqueo de ejemplo | Pendiente |
| HU-14 | Confirmar adopción realizada | Entrega, carta, confirmación y bloqueo de ejemplo | Pendiente |
| HU-15 | Programar seguimientos | Campos y lista de revisiones de ejemplo | Pendiente |
| HU-16 | Registrar seguimientos | Formulario, confirmación e historial de ejemplo | Pendiente |
| HU-17 | Conservar catálogo y borradores con Room | Avisos y vistas sin conexión; no existe guardado local | Pendiente |
| HU-18 | Consultar ubicación con Google Maps | Mapa ilustrativo y vista de error; no usa Google Maps | Pendiente |

**Avance: 7 de 18 historias implementadas: 2 comprobadas en el proyecto real y 5 solo en emuladores.**

### HU-01 comprobada

- [x] Registro con nombre, correo y contraseña mediante Firebase Authentication.
- [x] Inicio de sesión y perfil con nombre y correo de la cuenta autenticada.
- [x] Cierre de sesión real y recuperación de la sesión guardada por Firebase.
- [x] Validación de campos y errores de correo duplicado y credenciales incorrectas.
- [x] Conservación de los campos al fallar y posibilidad de reintentar.
- [x] Envío único mientras está en curso y navegación solo después del resultado correcto.
- [x] Pruebas unitarias de validación y estado del formulario.
- [x] Pruebas del SDK Android contra Firebase Auth Emulator.
- [x] Recorrido de la interfaz en dispositivo físico, incluida recreación de actividad y reintento de acceso.
- [x] Registro, nombre, cierre, acceso incorrecto/correcto y recuperación de sesión contra el proyecto real `huellitas-chalco`.

Configuración y pruebas reproducibles en [docs/firebase-auth.md](docs/firebase-auth.md).

### HU-07 comprobada

- [x] Rama `codex/hu-07-shelter-access` creada.
- [x] Inicio de sesión de refugios conectado a Firebase Authentication.
- [x] Rol y refugios permitidos mediante perfiles y membresías activas de Firestore.
- [x] Protección de los 15 destinos administrativos, incluido el muestrario.
- [x] Selector limitado a los refugios autorizados y perfil con datos reales de la cuenta.
- [x] Retirada de acceso al cambiar de cuenta, cerrar sesión o revocar membresías.
- [x] Permisos verificados con el servidor; bloqueo al esperar, fallar o estar sin conexión.
- [x] Reglas de Firestore probadas con visitante, adoptante y dos refugios; autoautorización bloqueada.
- [x] 23 pruebas de reglas y 7 pruebas unitarias nuevas, sin fallos.
- [x] Pruebas del SDK y de la interfaz en la tableta, incluidos los 15 destinos privados.
- [x] Compilación y Android Lint sin errores.
- [x] Firestore creado en modo de producción, plan Spark, ubicación `nam5`.
- [x] Publicar las reglas probadas en el proyecto real y habilitar el índice de membresías.
- [x] Confirmar y activar la cuenta responsable mediante su membresía.
- [x] Acceso al refugio autorizado y rechazo de consultas a un refugio ajeno, comprobados con una sesión del proyecto real en la tableta.

Configuración, alcance y pruebas: [docs/shelter-access.md](docs/shelter-access.md). En el proyecto real, las escrituras de perritos, solicitudes y seguimientos permanecen bloqueadas. HU-08 añade el registro de perritos en los emuladores locales.

### HU-08 comprobada en emuladores

El propietario eligió mantener Spark y utilizar emuladores sin costo. La activación real de Storage requiere Blaze y queda pendiente.

- [x] Rama `codex/hu-08-publish-dogs` y commits separados por configuración, funcionalidad, corrección, pruebas y documentación.
- [x] Formulario con nombre, edad en meses/años, tamaño, sexo y descripción, con validaciones.
- [x] Selector del sistema, de 1 a 6 fotografías y portada inicial.
- [x] Copias temporales JPEG reducidas, sin metadatos originales; validación de formato y tamaño.
- [x] Revisión previa y confirmación solo después de subir fotografías y guardar en Firestore.
- [x] Ficha y control privado creados en una transacción; reintentos con el mismo identificador.
- [x] Conservación de datos y fotos al fallar; carga parcial y pérdida de conexión comprobadas.
- [x] Consulta mínima de los registros publicados y sus fotos desde Mis perritos.
- [x] Reglas que exigen membresía activa, validan campos y aíslan datos y fotografías entre refugios.
- [x] 8 pruebas unitarias nuevas, 38 pruebas de reglas y 5 pruebas Android del SDK, sin fallos.
- [x] Regresión de autenticación y 15 destinos privados, compilación normal y Android Lint sin errores.
- [x] Instalación separada **Huellitas Chalco (local)** en la tableta.
- [x] Recorrido de interfaz en la tableta: validación, imagen ficticia entregada por el selector de prueba, revisión, regreso al formulario, publicación y consulta del detalle guardado.
- [ ] Activación y comprobación de Storage en Firebase real, pospuestas para mantener Spark.

Instrucciones y alcance: [docs/dog-publication.md](docs/dog-publication.md). HU-09 incorpora la salud y HU-10 la edición general; HU-02 conecta el catálogo público y los borradores persistentes siguen pendientes.

### HU-09 comprobada en emuladores

- [x] Rama `codex/hu-09-dog-health`, con commits separados `feat:`, `fix:`, `test:` y `docs:`.
- [x] Vacunación, desparasitación y esterilización independientes: Sí, No o Sin información; valor inicial Sin información.
- [x] Captura en el registro y comprobación en la revisión previa a publicar.
- [x] Edición de salud desde Mis perritos, persistida en Firestore con fecha del servidor.
- [x] Confirmación tras guardar, bloqueo de doble envío y conservación de selecciones ante errores.
- [x] Detección de cambios de otro responsable, con recarga explícita antes de guardar de nuevo.
- [x] Reintento idempotente y falta de conexión comprobados con el SDK Android.
- [x] Ficha pública por ID con la salud guardada; lectura sin sesión comprobada.
- [x] Reglas que aíslan refugios, admiten las 27 combinaciones y rechazan estados inválidos, fechas falsas y cambios en otros campos.
- [x] 37 pruebas unitarias totales, 44 pruebas de reglas y 28 pruebas Android de salud, registro y regresión de acceso, sin fallos.
- [x] Compilación normal/local y Android Lint sin errores.
- [ ] Publicación y comprobación de HU-09 en Firebase real, pospuestas para conservar Spark.

Uso, datos y pruebas: [docs/dog-health.md](docs/dog-health.md). La app local requiere los emuladores de la computadora; HU-02 conecta el catálogo real; HU-03 completa también la ficha pública.

### HU-10 comprobada en emuladores

- [x] Rama `codex/hu-10-edit-dogs`, con commits separados de datos y reglas, interfaz, pruebas y documentación.
- [x] Carga de los datos y fotografías originales del registro seleccionado.
- [x] Edición de nombre, edad en meses/años, tamaño, sexo y descripción, con validaciones del alta.
- [x] Agregar y quitar fotografías, elegir portada y conservar de una a seis referencias distintas.
- [x] Vista previa, regreso al formulario y cancelación con confirmación cuando hay cambios.
- [x] Carga de fotos nuevas seguida de guardado transaccional, sin modificar la ficha ante una carga parcial.
- [x] Confirmación tras guardar, bloqueo de doble envío y reintentos con los mismos datos y rutas.
- [x] Detección de conflictos y recarga explícita; conservación de cambios concurrentes de salud.
- [x] Folio, refugio, creación, salud, estado de adopción y control privado conservados.
- [x] Actualización en Mis perritos y ficha pública por ID, comprobada sin sesión mediante el SDK.
- [x] Reglas que bloquean edición ajena, rutas inválidas y sobrescritura de fotografías en uso.
- [x] 46 pruebas unitarias totales, 55 de reglas y 35 Android de registro, salud, edición y regresión de acceso, sin fallos.
- [x] Compilación normal/local y Android Lint sin errores.
- [ ] Publicación y comprobación de HU-10 en Firebase real, pospuestas para conservar Spark.

Uso, datos, límites y pruebas: [docs/dog-editing.md](docs/dog-editing.md). HU-02 conecta el catálogo general a los registros actualizados. Quitar una foto retira su referencia; la limpieza automática de objetos remotos queda pendiente.

### HU-02 comprobada en emuladores

- [x] Rama `codex/hu-02-public-catalog`, con commits separados `chore:`, `feat:`, `test:` y `docs:`.
- [x] Consulta sin sesión de perritos publicados, disponibles o en proceso.
- [x] Selección de refugios activos de Chalco, Estado de México; exclusión de otros municipios, adoptados y borradores.
- [x] Tarjetas con portada de Storage, nombre, edad, tamaño, sexo, refugio y estado.
- [x] Búsqueda por nombre o refugio sin distinguir mayúsculas o acentos; filtros combinados por tamaño, sexo y edad.
- [x] Carga, error con reintento, catálogo vacío y búsqueda sin coincidencias diferenciados.
- [x] Apertura del perrito seleccionado mediante su ID y regreso conservando la búsqueda.
- [x] Actualización del catálogo abierto al editar, adoptar o desactivar un refugio.
- [x] Lectura pública sin permisos administrativos, comprobada con reglas y SDK Android.
- [x] 52 pruebas unitarias totales, 59 de reglas y 44 Android distintos de catálogo y regresión, sin fallos tras corregir el bucket de las imágenes de prueba.
- [x] Compilación normal/local y Android Lint sin errores.
- [ ] Despliegue de índices y comprobación del catálogo con fotografías en Firebase real, pospuestos para conservar Spark.

Uso, consultas, límites y pruebas: [docs/public-catalog.md](docs/public-catalog.md). Room y el catálogo persistente sin conexión siguen pendientes en HU-17.

### HU-03 comprobada en emuladores

- [x] Rama `codex/hu-03-public-dog-detail`, con commits separados de datos, interfaz, corrección, pruebas y documentación.
- [x] Ficha del ID seleccionado con edad, tamaño, sexo, descripción, estado y salud guardados.
- [x] Galería de todas las fotografías, contador y navegación con extremos deshabilitados.
- [x] Nombre, zona pública y horario del refugio asociado; información adicional y regreso al mismo perrito.
- [x] Consulta sin sesión; rechazo de refugios inactivos, inexistentes o de otro municipio.
- [x] Carga, error, tiempo de espera y reintento; retirada de información si el registro deja de estar disponible.
- [x] Imagen inválida con reintento y retirada de la imagen anterior al cambiar de fotografía.
- [x] 60 pruebas unitarias totales y 15 pruebas Android de ficha, catálogo y regresión, sin fallos.
- [x] Compilación normal/local y Android Lint sin errores; reglas vigentes conservadas.
- [ ] Comprobación de HU-03 con fotografías en Firebase real, pospuesta para conservar Spark.

Datos, uso y pruebas: [docs/public-dog-detail.md](docs/public-dog-detail.md). Los requisitos se conectan en HU-04 y Google Maps en HU-18.

## Orden para implementarlas una por una

Firebase Authentication y la base Firestore están configurados. HU-07 está desplegada y comprobada con la cuenta autorizada en la tableta. HU-08 está comprobada con servicios locales; su activación en la nube está pospuesta para conservar Spark. HU-09 y HU-10 también están comprobadas en emuladores. HU-02 ya está comprobada en emuladores. HU-03 ya está comprobada en emuladores. Continúa HU-04 para conectar los requisitos del refugio. El diseño original se conserva como referencia.

1. [x] **HU-01 — Crear cuenta e iniciar sesión.** Crear usuarios y autenticar con Firebase Authentication. Validar campos, mostrar errores junto al campo y conservar los datos al fallar. Comprobar registro, acceso correcto, acceso incorrecto y cierre de sesión.
2. [x] **HU-07 — Autorizar al refugio.** Identificar el rol y el refugio de la cuenta. Restringir también el acceso a los datos: cada refugio administra únicamente sus perritos, solicitudes y seguimientos. Comprobar cuentas de adoptante, visitante y dos refugios distintos.
3. [x] **HU-08 — Registrar y publicar perritos (comprobada en emuladores).** Validar nombre, edad aproximada, tamaño, sexo, descripción y al menos una fotografía. Subir fotografías a Storage y guardar datos en Firestore. Comprobar reintentos de carga y que no se publique un registro incompleto.
4. [x] **HU-09 — Guardar el estado de salud (comprobada en emuladores).** Persistir vacunación, desparasitación y esterilización como Sí, No o Sin información, con Sin información como valor inicial. Comprobar que los valores guardados aparezcan en el detalle público.
5. [x] **HU-10 — Editar mis perritos (comprobada en emuladores).** Cargar y corregir los datos guardados y fotografías, mostrar vista previa y guardar cambios. Reflejarlos en Mis perritos y la ficha pública por ID; HU-02 también los refleja en el catálogo general. Comprobar que otra cuenta de refugio no pueda editar ese registro.
6. [x] **HU-02 — Consultar el catálogo real (comprobada en emuladores).** Leer perritos de Chalco desde Firestore. Mostrar fotografía, nombre, edad, tamaño y refugio de los disponibles o en proceso. Comprobar que los adoptados queden fuera del catálogo.
7. [x] **HU-03 — Consultar el detalle real (comprobada en emuladores).** Abrir el registro seleccionado y cargar sus fotografías, edad, tamaño, sexo, descripción, refugio, estado de adopción y salud. Comprobar que los datos correspondan al perrito elegido.
8. [ ] **HU-04 — Mostrar los requisitos del refugio.** Obtener los requisitos correspondientes al refugio del perrito y los pasos de adopción. Permitir consultarlos sin cuenta e iniciar la solicitud desde esa pantalla. Mantener identificados los datos de demostración.
9. [ ] **HU-05 — Enviar una solicitud real.** Validar contacto, hogar, otras mascotas y motivos; conservar las respuestas entre pasos y permitir revisarlas. Exigir sesión para enviar. Confirmar el folio solo después de guardar y comprobar que reintentar no duplique la solicitud ni borre las respuestas si falla.
10. [ ] **HU-06 — Consultar mis solicitudes reales.** Obtener únicamente las solicitudes de la cuenta actual, sus respuestas y cambios de estado con fecha. Mostrar Enviada, En revisión, Aprobada o No aprobada y el siguiente paso correspondiente. Comprobar que otra cuenta no pueda leerlas.
11. [ ] **HU-12 — Consultar solicitudes recibidas.** Obtener solo solicitudes de perritos del refugio actual, con solicitante, perrito, fecha, estado y respuestas. Comprobar la vista sin registros y el aislamiento entre refugios.
12. [ ] **HU-13 — Evaluar y guardar decisiones.** Aplicar Enviada → En revisión → Aprobada o No aprobada, con confirmación. Permitir una sola solicitud aprobada por perrito. Al aprobar, cambiarlo a En proceso y bloquear solicitudes nuevas. Comprobar también intentos simultáneos de aprobar dos solicitudes.
13. [ ] **HU-14 — Registrar la entrega y adopción.** Exigir solicitud aprobada, fecha real de entrega y confirmación de carta firmada. Al confirmar, marcar al perrito Adoptado, impedir otra adopción y pasar las solicitudes restantes a No aprobada con su motivo. Comprobar que desaparezca del catálogo público.
14. [ ] **HU-15 — Programar revisiones.** Desde una adopción registrada, guardar fecha y tipo de revisión: visita, llamada o videollamada. Mostrar las pendientes y permitir abrir su registro de seguimiento.
15. [ ] **HU-16 — Guardar seguimientos.** Persistir fecha, adaptación, observaciones y fotografía opcional. Confirmar únicamente tras guardar, actualizar el historial, marcar la revisión como realizada y permitir programar la siguiente o cerrar el seguimiento.
16. [ ] **HU-17 — Conservar catálogo y borradores con Room.** Implementar entidades, DAO y base local. Guardar el último catálogo y borradores separados por cuenta; restaurarlos al volver a abrir la app. Comprobar uso sin conexión, aviso de información desactualizada, ausencia de caché y eliminación del borrador solo tras confirmar el envío correcto.
17. [ ] **HU-18 — Mostrar la ubicación con Google Maps.** Cargar la zona pública aproximada del refugio sin mostrar domicilios privados. Conservar nombre y zona si el mapa falla y permitir reintentar. Identificar los datos ficticios como demostración.
18. [ ] **HU-11 — Conectar Gemini.** Generar una propuesta basada exclusivamente en los datos capturados, sin inventar características ni información médica. Permitir aceptar, editar, descartar y escribir manualmente si falla. Comprobar que nunca se publique automáticamente.

## Cuándo marcar una historia como terminada

- El comportamiento funciona con datos guardados y cuentas autenticadas de prueba, no únicamente con `PreviewData` o `UiVariant`. Si el propietario elige emuladores, se debe indicar expresamente que falta la comprobación en la nube.
- Se cumplen los criterios de aceptación correspondientes y las restricciones de acceso.
- Se comprueban el caso correcto, los errores relevantes y la conservación o actualización de la información.
- Las pantallas de éxito aparecen después de que la operación haya terminado correctamente.

HU-01/HU-07 usan el proyecto real. HU-02/HU-03/HU-08/HU-09/HU-10 usan servicios locales en la instalación de pruebas. Las demás historias siguen pendientes; todavía no se guardan solicitudes, evaluaciones, adopciones ni seguimientos reales.
