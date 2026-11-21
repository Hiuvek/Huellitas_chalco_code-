# HU-02 — Catálogo público de perritos de Chalco

La pantalla normal del catálogo consulta Firestore sin exigir una cuenta. Cada tarjeta muestra la portada guardada en Storage, nombre, edad en meses, tamaño, sexo, nombre del refugio y estado. Al seleccionarla abre la ficha real mediante su identificador.

Se muestran registros con `publicado=true` y estado **Disponible** o **En proceso**, vinculados a refugios con `activo=true`, `municipio=Chalco` y `entidad=Estado de México`. Se excluyen borradores, adoptados, refugios inactivos y otros municipios. Las variantes del muestrario conservan ejemplos identificados; el catálogo normal no usa `PreviewData` como sustituto de una consulta fallida.

## Búsqueda y filtros

La búsqueda consulta nombre o refugio, ignora mayúsculas, acentos y espacios al principio o al final. Los filtros por tamaño, sexo y edad se combinan con el texto. **Cachorro** comprende menos de 12 meses; **Adulto**, desde 12 meses. Limpiar búsqueda y filtros restaura el catálogo completo. La selección se conserva al abrir una ficha y regresar durante la sesión.

El catálogo muestra por separado carga, error con reintento, ausencia de registros y búsqueda sin coincidencias. Los cambios de fichas y la retirada por adopción se reflejan mediante listeners; desactivar un refugio también retira sus tarjetas del catálogo abierto. Los errores de fotografía permiten reintentar su descarga.

## Entorno elegido y comprobación manual

Por decisión del propietario se mantienen Spark y los emuladores. Esta historia se comprueba exclusivamente en `demo-huellitas-chalco`, con la instalación **Huellitas Chalco (local)**. No se han desplegado reglas ni índices nuevos en Firebase real.

1. Arrancar los tres servicios y preparar las cuentas siguiendo [dog-publication.md](dog-publication.md).
2. En otra terminal, desde la raíz del repositorio, ejecutar:

   ```bash
   npm --prefix tools/firebase run seed:catalog
   ```

3. Abrir el catálogo sin iniciar sesión. Buscar `HU02`: deben aparecer **Lunita HU02** y **Óscar HU02** con sus refugios y fotografías de prueba.
4. Filtrar por Macho: debe quedar Óscar. Buscar `oscar` también debe encontrarlo. Abrirlo y comprobar sus datos, después regresar.
5. Publicar o editar un perrito desde un refugio autorizado y volver al catálogo para comprobar el registro actualizado.

El script crea seis fichas y tres refugios ficticios solo en los emuladores. Además de las dos fichas visibles, prepara un adoptado, un borrador, otro municipio y un refugio inactivo para comprobar las exclusiones. La imagen de prueba es un JPEG generado de color uniforme, no representa un animal real. El script reemplaza únicamente sus identificadores `hu02-*`; no modifica cuentas personales ni conecta al proyecto real.

## Datos, consultas y alcance

| Origen | Consulta y uso |
| --- | --- |
| `perritos` | `publicado == true` y `estado in [Disponible, En proceso]`; lectura de las fichas públicas. |
| `refugios` | `activo == true`, `municipio == Chalco` y `entidad == Estado de México`; nombre y selección territorial. |
| Storage | Lectura de la portada referenciada por cada ficha, con las reglas vigentes. |

No se duplican nombres de refugio en la ficha ni se modifican las reglas de escritura. Se añaden definiciones de índices compuestos a `firestore.indexes.json` para ambas consultas; su despliegue y verificación en la nube quedan pendientes junto con la activación real de las fotografías. El emulador no verifica todos los requisitos de índices de producción.

La unión entre fichas y refugios y los filtros de búsqueda se ejecutan en el cliente. La consulta recibe los perritos públicos disponibles o en proceso y después conserva solo los de los refugios territoriales admitidos. Está pensada para el volumen del proyecto escolar; no implementa paginación ni búsqueda de texto en un servidor dedicado.

Se requiere confirmación del servidor para presentar el catálogo como actual. Si no llega en 12 segundos, se muestra un error y se permite reintentar. El listener puede recuperarse al regresar la conexión. **Todavía no existe catálogo persistente con Room**: la consulta sin conexión y la indicación de datos desactualizados corresponden a HU-17. HU-03 completa la [ficha pública](public-dog-detail.md); los requisitos se conectan en HU-04.

## Pruebas

Verificación completada: **52 pruebas unitarias, 59 de reglas y 44 Android distintos** (8 de catálogo y 36 de regresión), compilación normal/local y Android Lint sin errores.

Las pruebas unitarias verifican búsqueda, acentos, combinación de filtros, frontera de edad, carga/vacío/error, cambios en vivo y reintento sin perder la selección. Las reglas comprueban la consulta sin sesión y rechazan consultas más amplias o intentos de escribir. Las pruebas Android usan los SDK reales contra emuladores: portada, exclusiones, cambios de nombre, adopción, desactivación del refugio y recuperación de conexión. La interfaz recorre búsqueda, filtros, selección y regreso; también comprueba estados de carga, error, vacío y ausencia de coincidencias.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
npm --prefix tools/firebase run test:rules
```

Detener los emuladores locales antes de la prueba de reglas, ya que inicia servicios propios y limpia sus datos. Después reiniciarlos y ejecutar `seed:local` y `seed:catalog` antes de las pruebas Android `FirebaseCatalogSdkTest` y `CatalogFlowTest`.

Fuentes: [consultas de Firestore](https://firebase.google.com/docs/firestore/query-data/queries) y [actualizaciones mediante listeners](https://firebase.google.com/docs/firestore/query-data/listen).
