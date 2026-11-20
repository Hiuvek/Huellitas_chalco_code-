# Checklist de funcionalidades — Huellitas Chalco

Revisión del 20 de noviembre de 2026. Referencia: las 18 historias del Excel corregido, conservando sus identificadores.

La entrega actual es una interfaz con datos de ejemplo. Escribir en campos, cambiar selecciones, abrir diálogos o llegar a una pantalla de confirmación no guarda una operación real. Una historia se marcará como terminada cuando su comportamiento y sus criterios de aceptación estén implementados y comprobados.

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
| HU-01 | Crear cuenta e iniciar sesión | Formularios y error de acceso | Pendiente |
| HU-02 | Consultar catálogo de perritos en Chalco | Catálogo con registros de ejemplo | Pendiente |
| HU-03 | Consultar detalle del perrito | Fotografías, características, salud y refugio de ejemplo | Pendiente |
| HU-04 | Consultar requisitos y pasos | Requisitos de ejemplo y acceso al formulario | Pendiente |
| HU-05 | Enviar solicitud de adopción | Cinco pasos, revisión y confirmación de ejemplo | Pendiente |
| HU-06 | Consultar mis solicitudes | Lista, detalle y estados de ejemplo | Pendiente |
| HU-07 | Acceso autorizado del refugio | Acceso y pantalla de restricción de ejemplo | Pendiente |
| HU-08 | Registrar y publicar perritos | Formulario, fotografías simuladas y vista previa | Pendiente |
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

Ninguna de las 18 historias tiene todavía su funcionalidad real completa. Esto corresponde al alcance solicitado: desarrollar primero la interfaz.

## Orden para implementarlas una por una

Primero se necesita configurar Firebase Authentication, Cloud Firestore y Firebase Storage en el proyecto, y conectar los modelos con los repositorios. El diseño previo de la base de datos sirve de referencia; todavía no está integrado en la app.

1. [ ] **HU-01 — Crear cuenta e iniciar sesión.** Crear usuarios y autenticar con Firebase Authentication. Validar campos, mostrar errores junto al campo y conservar los datos al fallar. Comprobar registro, acceso correcto, acceso incorrecto y cierre de sesión.
2. [ ] **HU-07 — Autorizar al refugio.** Identificar el rol y el refugio de la cuenta. Restringir también el acceso a los datos: cada refugio administra únicamente sus perritos, solicitudes y seguimientos. Comprobar cuentas de adoptante, visitante y dos refugios distintos.
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

- El comportamiento funciona con datos guardados y cuentas reales de prueba, no únicamente con `PreviewData` o `UiVariant`.
- Se cumplen los criterios de aceptación correspondientes y las restricciones de acceso.
- Se comprueban el caso correcto, los errores relevantes y la conservación o actualización de la información.
- Las pantallas de éxito aparecen después de que la operación haya terminado correctamente.

La navegación, las selecciones y los campos actuales son estado de presentación. Las confirmaciones y los mensajes de guardado actuales son ejemplos visuales.
