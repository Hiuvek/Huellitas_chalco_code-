# HU-17 — Catálogo y borradores con Room

Room 2.8.5 y KSP 2.3.12 generan y verifican la base local versión 1. El esquema exportado se conserva en `app/schemas`. Se utilizan las tres tablas y columnas del modelo original:

| Tabla | Uso |
| --- | --- |
| cache_refugios | Datos públicos del refugio, zona, horario y configuración de requisitos/pasos. |
| cache_perritos | Últimas fichas públicas de perritos disponibles o en proceso; FK a cache_refugios con borrado en cascada. |
| borradores | PK compuesta propietarioLocal + tipo + referenciaId; contenido JSON y fecha del último guardado. |

`requisitosJson` conserva requisitos, pasos y marca de demostración; `fotosJson` conserva referencias ordenadas y marca de demostración. `actualizadoEn` representa cuándo se guardó la copia confirmada. La sustitución del catálogo es una transacción completa, sin mezclar refugios y perritos de diferentes lecturas.

## Consulta sin conexión

Solo una respuesta confirmada del servidor reemplaza la copia pública. Mientras se actualiza, o si no hay conexión, se muestra la última copia con su fecha y el aviso de que la disponibilidad puede haber cambiado. Búsqueda y filtros siguen funcionando. Sin una copia previa se muestra un error explícito y Reintentar, sin perritos de ejemplo.

La ficha y los requisitos también pueden consultarse desde la copia guardada. Una solicitud puede prepararse como borrador; el envío se deshabilita mientras la ficha esté desactualizada. Es necesario actualizarla con el servidor antes de enviar.

Las fotografías públicas que se han consultado se guardan en una caché privada de archivos. Solo se admiten rutas pertenecientes al catálogo público guardado. Las fotos no visitadas pueden necesitar conexión; si no están disponibles se mantiene la ficha y su opción de reintento. Solicitudes, membresías, decisiones y fotografías privadas de seguimientos no entran en esta caché. Al sustituir el catálogo se eliminan sus fotos que ya no están referenciadas.

## Borradores

Las solicitudes usan tipo SOLICITUD y referencia del perrito. Los registros nuevos del refugio usan tipo PERRITO y referencia del refugio; hay un borrador de alta por cuenta y refugio. No se comparten entre cuentas y no se envían automáticamente.

Se restauran respuestas, paso y, si hubo un envío ambiguo, el mismo identificador e intento bloqueado para reintentar sin duplicar. El alta conserva características, salud y fotos. Las copias JPEG de los borradores usan archivos privados duraderos, no la carpeta temporal de caché.

Se avisa cuando Room confirma el guardado. Si no se puede recuperar o guardar el borrador, el envío se bloquea y se pide volver a abrir la pantalla; no se sustituye silenciosamente por uno nuevo. Un error de red conserva el borrador. Se elimina después del envío confirmado; el descarte explícito del alta también elimina sus fotos y borrador. Cerrar la pantalla no los elimina.

Los borradores del refugio se muestran tras validar su membresía con el servidor. Room no concede acceso administrativo sin conexión ni reemplaza las reglas de HU-07. La base y las fotos de borradores se excluyen de las copias automáticas y transferencia del dispositivo.

## Verificación

Pruebas Android con una base de archivo real, cerrada y reabierta: esquema, FK y reversión de una sustitución incompleta; caché con filtros y requisitos; ausencia de caché; aislamiento de claves; conservación de un intento fallido y eliminación tras confirmación; restauración del alta y su JPEG después de liberar su ViewModel; exclusión de evidencias privadas de la caché pública. También se comprueban los flujos de solicitud, publicación y bloqueo de los destinos privados.

Referencias: [Room](https://developer.android.com/jetpack/androidx/releases/room), [KSP 2.3.12](https://github.com/google/ksp/releases/tag/2.3.12). No se cambian el plan Spark ni las reglas de Firebase en esta historia.

Resultado: 99 pruebas unitarias y 28 Android sin fallos. La prueba adicional de interfaz desconecta Firestore y recorre catálogo, búsqueda, ficha, requisitos y guardado del borrador. Compilaciones normal/local y Android Lint correctos. Las 89 pruebas de reglas anteriores siguen como referencia; HU-17 no modifica las reglas.
