# Checklist de funcionalidades — Huellitas Chalco

Revisión del 21 de noviembre de 2026. Referencia: las 18 historias del Excel corregido, conservando sus identificadores.

HU-01 usa Firebase Authentication para registrar cuentas e iniciar y cerrar sesión. HU-07 usa membresías activas de Cloud Firestore para autorizar al responsable y limitarlo a sus refugios; su acceso y el rechazo de consultas a un refugio ajeno están comprobados con una sesión del proyecto real. HU-08 tiene registro, fotografías y publicación conectados a emuladores; falta cerrar su recorrido de interfaz en la tableta. Las otras 15 historias conservan sus interfaces con datos de ejemplo.

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
| HU-02 | Consultar catálogo de perritos en Chalco | Catálogo con registros de ejemplo | Pendiente |
| HU-03 | Consultar detalle del perrito | Fotografías, características, salud y refugio de ejemplo | Pendiente |
| HU-04 | Consultar requisitos y pasos | Requisitos de ejemplo y acceso al formulario | Pendiente |
| HU-05 | Enviar solicitud de adopción | Cinco pasos, revisión y confirmación de ejemplo | Pendiente |
| HU-06 | Consultar mis solicitudes | Lista, detalle y estados de ejemplo | Pendiente |
| HU-07 | Acceso autorizado del refugio | Inicio de sesión real, verificación de membresías y bloqueo central | Implementada y comprobada |
| HU-08 | Registrar y publicar perritos | Formulario, fotos reales, revisión y consulta conectados a Firebase local | Código y pruebas de SDK listos; pendiente cierre de interfaz |
| HU-09 | Registrar estado de salud | Selectores de vacunación, desparasitación y esterilización | Pendiente |
| HU-10 | Revisar y editar mis perritos | Lista, edición y vista previa | Pendiente |
| HU-11 | Redactar descripciones con Gemini | Propuesta fija, aceptación, descarte y error de ejemplo | Pendiente |
| HU-12 | Consultar solicitudes recibidas | Lista, respuestas y vista vacía de ejemplo | Pendiente |
| HU-13 | Evaluar solicitudes | Revisión, decisión, confirmación y bloqueo de ejemplo | Pendiente |
| HU-14 | Confirmar adopción realizada | Entrega, carta, confirmación y bloqueo de ejemplo | Pendiente |
| HU-15 | Programar seguimientos | Campos y lista de revisiones de ejemplo | Pendiente |
| HU-16 | Registrar seguimientos | Formulario, confirmación e historial de ejemplo | Pendiente |
| HU-17 | Conservar catálogo y borradores con Room | Avisos y vistas sin conexión; no existe guardado local | Pendiente |
| HU-18 | Consultar ubicación con Google Maps | Mapa ilustrativo y vista de error; no usa Google Maps | Pendiente |

**Avance: 2 de 18 historias implementadas.**

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

### HU-08 — Pruebas locales

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
- [ ] Cerrar el recorrido de interfaz: validación, selección de imagen, revisión, publicación y consulta. La tableta se bloqueó durante la última ejecución.
- [ ] Activación y comprobación de Storage en Firebase real, pospuestas para mantener Spark.

Instrucciones y alcance: [docs/dog-publication.md](docs/dog-publication.md). La edición, salud, catálogo público y borradores persistentes continúan en sus historias correspondientes.

## Orden para implementarlas una por una

Firebase Authentication y la base Firestore están configurados. HU-07 está desplegada y comprobada con la cuenta autorizada en la tableta. HU-08 se está comprobando con servicios locales; su activación en la nube está pospuesta para conservar Spark. El diseño original se conserva como referencia.

1. [x] **HU-01 — Crear cuenta e iniciar sesión.** Crear usuarios y autenticar con Firebase Authentication. Validar campos, mostrar errores junto al campo y conservar los datos al fallar. Comprobar registro, acceso correcto, acceso incorrecto y cierre de sesión.
2. [x] **HU-07 — Autorizar al refugio.** Identificar el rol y el refugio de la cuenta. Restringir también el acceso a los datos: cada refugio administra únicamente sus perritos, solicitudes y seguimientos. Comprobar cuentas de adoptante, visitante y dos refugios distintos.
3. [ ] **HU-08 — Registrar y publicar perritos.** Validar nombre, edad aproximada, tamaño, sexo, descripción y al menos una fotografía. Subir fotografías a Storage y guardar datos en Firestore. Comprobar reintentos de carga y que no se publique un registro incompleto.
4. [ ] **HU-09 — Guardar el estado de salud.** Persistir vacunación, desparasitación y esterilización como Sí, No o Sin información, con Sin información como valor inicial. Comprobar que los valores guardados aparezcan en el detalle público.
5. [ ] **HU-10 — Editar mis perritos.** Cargar los datos guardados, permitir corregirlos y mostrar vista previa. Guardar cambios y reflejarlos en el catálogo. Comprobar que otra cuenta de refugio no pueda editar ese registro.
6. [ ] **HU-02 — Consultar el catálogo real.** Leer perritos de Chalco desde Firestore. Mostrar fotografía, nombre, edad, tamaño y refugio de los disponibles o en proceso. Comprobar que los adoptados queden fuera del catálogo.
7. [ ] **HU-03 — Consultar el detalle real.** Abrir el registro seleccionado y cargar sus fotografías, edad, tamaño, sexo, descripción, refugio, estado de adopción y salud. Comprobar que los datos correspondan al perrito elegido.
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

HU-01/HU-07 usan el proyecto real. HU-08 usa servicios locales en la instalación de pruebas. Las demás historias siguen como presentación; todavía no se guardan solicitudes, evaluaciones, adopciones ni seguimientos reales.
