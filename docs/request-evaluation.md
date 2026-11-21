# HU-13 — Evaluación de solicitudes

Desde una solicitud recibida se puede pasar Enviada → En revisión y después aprobar o no aprobar. Cada acción requiere confirmación con folio, solicitante y decisión. No aprobar requiere un motivo de 5 a 1000 caracteres, visible para la persona solicitante. No se cambian sus respuestas originales.

Cada cambio guarda estado, fecha del servidor e historial inmutable. Aprobar actualiza también `perritos_control.solicitudAprobadaId` y el perrito a En proceso en una transacción. Las reglas exigen las partes del cambio: no basta con escribir Aprobada en la solicitud o modificar el control por separado.

Dos decisiones simultáneas compiten por el mismo control del perrito. Solo una aprobación puede persistir; la otra conserva la solicitud En revisión e informa del conflicto. La validación del servidor de HU-05 bloquea solicitudes nuevas en cuanto el perrito pasa a En proceso.

Los registros de historial usan sufijos deterministas `-revision`, `-aprobada` y `-no-aprobada`. Reintentar una acción ya confirmada comprueba el historial del mismo actor y motivo, sin duplicarlo ni cambiar su fecha. Una acción distinta o un estado actualizado por otra persona requiere revisar la información actual.

La interfaz impide doble envío, conserva la decisión y el motivo ante errores y permite reintentar. Cerrar la confirmación no escribe. Las reglas siguen exigiendo perfil, membresía y refugio activos. Otro refugio, un visitante o el adoptante no pueden decidir.

## Comprobar en emuladores

Arrancar servicios y preparar `seed:local`, `seed:catalog` y `seed:requests`.

1. Entrar con `hu07-a@example.test` / `PruebaHU07!` y abrir una solicitud recibida.
2. Confirmar Pasar a En revisión; comprobar la fecha e historial nuevos.
3. Aprobar y confirmar: el perrito debe estar En proceso y no admitir otra aprobación.
4. Revisar la otra solicitud para el mismo perrito: no debe permitir una segunda aprobación persistida.
5. Con B, pasar su solicitud a revisión y No aprobar: capturar un motivo y comprobarlo con la cuenta adoptante en Mis solicitudes.

Volver a ejecutar `seed:requests` restablece exclusivamente los folios de demostración y su historial. Se conservan Spark y los emuladores; no se despliegan estas reglas al proyecto real. La entrega de la adopción se conecta en HU-14.

Verificación: **83 pruebas unitarias totales, 84 de reglas y 35 Android**, sin fallos; compilación normal/local y Android Lint sin errores.
