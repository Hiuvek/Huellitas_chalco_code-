# HU-12 — Solicitudes recibidas

La administración del refugio consulta `solicitudes` con `refugioId` igual al refugio seleccionado y muestra solicitante, perrito, folio, estado y fecha. Los filtros cuentan los registros recibidos, permiten Todas, Enviada, En revisión, Aprobada y No aprobada y distinguen lista vacía de un filtro sin coincidencias.

Cada tarjeta abre su ID real y carga las respuestas completas e historial cronológico. Las consultas de historial incluyen refugio y solicitud. No se cargan solicitudes de otros refugios para filtrarlas en el teléfono.

El control central de HU-07 exige sesión, perfil, refugio y membresía activos. Las reglas verifican también cada lectura. El modelo se separa por UID, refugio e ID; cambiar de refugio o cuenta deja de mostrar la información anterior. No se confirman respuestas desde la caché privada y un error permite reintentar. Los cambios de estado se implementan en HU-13.

## Entorno escolar

Se conservan Spark y los emuladores. Preparar `seed:local`, `seed:catalog` y `seed:requests`. Este último crea dos perritos y tres solicitudes ficticias: dos para el refugio A y una para B. No concede membresías ni modifica el proyecto real.

Entrar por Acceso refugios con `hu07-a@example.test` y contraseña local `PruebaHU07!`. Solicitudes debe mostrar las dos solicitudes de A; abrir una y comprobar las respuestas. La cuenta B solo verá la suya. Un adoptante no obtiene acceso administrativo, aunque sea propietario de una solicitud.

La preparación usa únicamente IDs `hu12-*` y folios de demostración `12000000-*`. Volver a ejecutarla restablece esos registros y sus entregas/revisiones de demostración para la revisión escolar. No debe usarse para conservar avances del escenario ficticio.

Verificación: **78 pruebas unitarias totales y 25 Android**, sin fallos; compilación normal/local y Android Lint sin errores. Se conservan las reglas probadas en HU-05.
