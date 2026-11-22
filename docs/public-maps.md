# HU-18 — Ubicación pública con Google Maps

Desde Ficha pública → Información del refugio → Ver en Google Maps se abre la zona autorizada usando Maps URLs. Android utiliza Google Maps cuando está disponible o el navegador. Esta variante no requiere clave de Maps ni activar facturación; no integra el SDK de mapas dentro de la app.

Se leen únicamente `zonaPublica`, municipio, entidad y el campo opcional `ubicacionAproximada` del refugio seleccionado. No se consultan domicilios ni ubicación del adoptante, no se solicita GPS y no se transmite contacto ni información de solicitudes.

Las coordenadas publicadas se validan como números finitos dentro de sus rangos y se redondean a tres decimales para la consulta aproximada. Si no existen, se busca la zona pública en Chalco, Estado de México. Sin zona ni coordenadas válidas no se inventa un punto ni se ofrece un enlace vacío. Los datos ficticios siguen identificados como demostración.

El nombre, la zona y el horario permanecen en la app. Si Android no puede abrir el enlace, se muestra el error y Reintentar. Si falla la red dentro de Google Maps o del navegador, ese servicio controla la carga; al volver, la información del refugio continúa disponible. La app no afirma que se hayan cargado los mosaicos de un mapa externo.

Room conserva también las coordenadas públicas opcionales dentro del JSON de configuración ya existente, sin agregar columnas ni cambiar el esquema. Las copias anteriores que no tienen coordenadas siguen funcionando con su zona publicada y aviso de copia guardada.

Esta implementación toma la opción sin facturación para mantener el alcance escolar. La variante de mapa incrustado requiere otra configuración y queda fuera de esta solución.

Referencia: [Maps URLs de Google](https://developers.google.com/maps/documentation/urls/get-started).

Verificación: 103 pruebas unitarias y 10 Android sin fallos, incluidos los dos casos nuevos de apertura mediante Intent y reintento; regresión de persistencia y consulta sin Firestore. Compilación local y Android Lint correctos.
