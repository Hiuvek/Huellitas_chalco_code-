# HU-06 — Mis solicitudes

La pestaña Solicitudes consulta Firestore con el UID de la sesión. Presenta los folios enviados, perrito, estado, fecha y siguiente paso. Cada tarjeta abre el ID real; la confirmación de HU-05 también permite **Ver mi solicitud**. Sin sesión se pide iniciar sesión.

El detalle muestra las respuestas originales completas, fechas del servidor, motivo de rechazo cuando exista e historial cronológico. No inventa cambios ni entrevistas agendadas. Enviada, En revisión, Aprobada y No aprobada tienen indicaciones distintas. Las decisiones se conectan en HU-13.

Las consultas incluyen `usuarioUid` tanto en solicitudes como en historial; el historial añade `solicitudId`. No se cargan solicitudes ajenas para filtrarlas en la pantalla. Las reglas de HU-05 ya protegen estas lecturas. Una cuenta nueva puede consultar sus datos sin obtener permisos administrativos.

Cerrar sesión o cambiar de cuenta cancela la consulta anterior y retira sus respuestas. Los modelos se separan por UID e ID. Un permiso revocado, dato incompleto o falta de confirmación del servidor muestra error y permite reintentar; la caché privada no se presenta como una respuesta confirmada.

El nombre actual del perrito se consulta cuando está disponible públicamente. Si no se puede leer su ficha, la solicitud sigue visible con el identificador del perrito: su historial y sus respuestas no dependen de que permanezca en el catálogo. La pantalla no muestra una foto de ejemplo como si correspondiera al registro. Las consultas y la ordenación son adecuadas para el volumen escolar; la paginación queda fuera de esta entrega.

## Comprobar en emuladores

1. Preparar `seed:local` y `seed:catalog` y abrir Huellitas Chalco (local).
2. Enviar una solicitud para Lunita HU02 y pulsar Ver mi solicitud.
3. Abrir Tus respuestas y comprobar contacto, hogar, mascotas y motivos capturados.
4. Regresar a Mis solicitudes y abrir la tarjeta del mismo folio.
5. Cerrar sesión: no deben mostrarse respuestas privadas. Entrar con otra cuenta: no debe aparecer el folio anterior.
6. Desconectar los emuladores y reintentar: debe mostrarse error sin confirmar datos de caché. Restablecer la conexión y reintentar.

Se conserva Spark. No se despliegan reglas ni servicios a Firebase real. Ver [adoption-application.md](adoption-application.md) para preparar una solicitud y [dog-publication.md](dog-publication.md) para arrancar los servicios locales.

Verificación: **78 pruebas unitarias totales y 9 Android**, sin fallos; compilación normal/local y Android Lint sin errores. Se mantienen las reglas comprobadas en HU-05 (73 pruebas).
