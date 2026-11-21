# HU-04 — Requisitos y pasos del refugio

La ficha del perrito y la información pública del refugio permiten abrir **Conocer requisitos**. La pantalla usa el identificador del perrito seleccionado para consultar su refugio y mostrar los campos `requisitos` y `pasos` guardados en Firestore. No requiere iniciar sesión.

Las condiciones y los pasos conservan su orden. Los registros locales incluyen un aviso de demostración. Un perrito En proceso puede consultarse, con aviso de que no admite nuevas solicitudes. Una baja o desactivación retira también la información de requisitos.

Una configuración ausente, vacía, con valores que no sean texto o con entradas en blanco se considera incompleta; no se omiten condiciones silenciosamente ni se presentan ejemplos como requisitos reales. En ese caso se muestra el aviso y el acceso a la información del refugio. Los errores de consulta conservan la posibilidad de reintentar.

## Estado de esta entrega

La lectura y el inicio están conectados. **Iniciar solicitud** abre HU-05 para el mismo perrito; si no hay sesión, permite iniciar sesión o registrarse y regresar al formulario. El botón se deshabilita para perritos En proceso o una configuración incompleta. Véase [adoption-application.md](adoption-application.md).

## Comprobar

1. Arrancar los emuladores y ejecutar `npm --prefix tools/firebase run seed:local` y `npm --prefix tools/firebase run seed:catalog`.
2. En **Huellitas Chalco (local)**, consultar el catálogo sin sesión y abrir Lunita HU02.
3. Pulsar **Conocer requisitos**. Deben mostrarse sus tres condiciones y los cinco pasos del refugio, con el aviso de demostración.
4. Regresar: se conserva el perrito seleccionado.

La preparación solo cambia documentos ficticios `hu02-*` del emulador. No se despliegan reglas ni datos a Firebase real y se conserva Spark.

Las pruebas comprueban listas completas e incompletas, identidad del refugio, lectura pública con el SDK Android y navegación desde el catálogo. No cambian las reglas de autorización verificadas en HU-02.

Verificación de esta entrega: **64 pruebas unitarias y 16 Android**, compilación normal/local y Android Lint sin errores.
