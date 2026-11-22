# HU-16 — Resultados de seguimientos

Desde Seguimientos → Programados → Abrir revisión se capturan fecha real (AAAA-MM-DD), adaptación y observaciones de 15 a 2000 caracteres. La fotografía es opcional; el selector y el importador del alta convierten la imagen a JPEG de hasta 2 MB sin metadatos originales.

Se debe elegir programar la siguiente revisión (fecha posterior y tipo obligatorio) o cerrar este seguimiento. Cerrar guarda el resultado sin crear otra cita; no cancela otras revisiones ya programadas.

La función autenticada `recordFollowUp` comprueba perfil, membresía y refugio activos, revisión pendiente y adopción correspondiente. La fecha realizada no puede ser futura ni anterior a la entrega. Se conserva por separado la fecha originalmente programada.

En una sola transacción crea `seguimientos/{revisionId}`, marca la revisión REALIZADA con su seguimientoId y, si se solicitó, crea la siguiente revisión. El ID de la siguiente es SHA-256 de revisionId + NUL + `next`, para repetir la misma operación sin duplicar citas. El resultado conserva los campos del modelo original; no agrega columnas de cierre o vínculos ficticios. Otro intento con un resultado distinto se rechaza.

La foto se carga primero a `refugios/{refugioId}/seguimientos/{revisionId}/{uuid}.jpg`. Es privada del refugio y no se sobrescribe ni elimina desde el cliente. El backend verifica ruta, tamaño, tipo y marcadores JPEG antes de guardar la referencia. Android decodifica y recodifica la imagen real. Una carga que no llega a confirmarse puede dejar un objeto privado sin referencia; su limpieza administrativa queda pendiente. No se usan enlaces públicos de descarga.

Una respuesta ambigua mantiene el intento, foto y datos para reintentar. La app consulta el resultado confirmado antes de intentar cargar de nuevo una evidencia inmutable. La pantalla de éxito depende de la respuesta del servidor o del resultado confirmado en Firestore; nunca de un temporizador.

El Historial consulta solo el refugio autorizado y muestra fecha, adaptación, observaciones, foto opcional y la siguiente revisión vinculada. Los registros incompletos heredados de las primeras semillas de acceso se cuentan y se avisan; no se inventa su fecha ni se presentan como resultados válidos.

## Preparación y prueba

Se mantienen Spark y los emuladores gratuitos, con los puertos y semillas descritos en [review-scheduling.md](review-scheduling.md). El backend nuevo se carga desde `functions`; no se despliega en la nube. Para conservar un escenario no ejecutar seed:requests, que restablece sus solicitudes, adopciones y revisiones ficticias.

Entrar como responsable A/B, abrir una cita, comprobar validaciones, capturar el resultado y confirmar. Debe desaparecer de Programados y aparecer en Historial. Si se eligió otra revisión, debe aparecer una sola cita nueva. Cerrar no crea ninguna. Otra cuenta de refugio, adoptante o visitante no puede leer ni escribir la evidencia privada.

Reglas de inmutabilidad de objetos: [referencia oficial de Firebase Storage](https://firebase.google.com/docs/reference/security/storage).

## Verificación realizada

99 pruebas unitarias, 89 de reglas y 17 del backend sin fallos. En Android pasaron 18 pruebas de agenda/acceso y las 2 nuevas del SDK e interfaz después de corregir la consulta previa: ahora comprueba los resultados del refugio autorizado, evitando una consulta por ID inexistente que el servidor denegaba. El recorrido de la tableta comprueba selector, JPEG real, guardado, historial y siguiente revisión; el SDK comprueba cierre, reintento y aislamiento. Compilación local y Android Lint correctos.
