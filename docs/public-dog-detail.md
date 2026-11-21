# HU-03 — Ficha pública del perrito

La ficha se abre con el identificador del registro elegido en el catálogo o en Mis perritos. Consulta la fotografía, edad en meses, tamaño, sexo, descripción, estado y salud guardados en Firebase, junto con la información del refugio asociado. No utiliza datos de otro perrito ni completa campos con ejemplos.

## Uso

1. Abrir el catálogo local y buscar `Lunita HU02` después de ejecutar `npm --prefix tools/firebase run seed:catalog`.
2. Abrir la tarjeta. Comprobar edad, tamaño, sexo, descripción y los tres estados de salud.
3. Usar **Siguiente** y **Anterior** para consultar todas las fotografías; los extremos quedan deshabilitados y se muestra el número de imagen actual.
4. Abrir **Información del refugio**. Debe mostrar el nombre, municipio, zona pública y horario de ese refugio; regresar conserva el perrito elegido.

Las dos fotografías de la ficha de prueba son JPEG de color uniforme para verificar la navegación; no representan animales reales. Los registros publicados desde la app usan las fotografías seleccionadas por el responsable.

## Datos y errores

| Documento | Datos usados |
| --- | --- |
| `perritos/{id}` | Nombre, edad, tamaño, sexo, descripción, fotografías ordenadas, salud, estado y referencia del refugio. |
| `refugios/{refugioId}` | Nombre, municipio, entidad, zona pública, horario e indicador de demostración. |

Solo se presenta el registro cuando está publicado, disponible o en proceso, y su refugio sigue activo en Chalco, Estado de México. Una baja, adopción, desactivación o referencia inexistente muestra un error y permite reintentar. Mientras falta la respuesta del servidor se muestra carga; después de 12 segundos sin confirmación se ofrece reintento. Los cambios de descripción y salud se actualizan durante la consulta.

Los campos públicos de zona u horario ausentes se muestran como no registrados; no se inventan direcciones ni horarios. La app no muestra un domicilio privado. Si una fotografía falla se puede reintentar su descarga; las demás imágenes siguen siendo seleccionables.

## Entorno y alcance

HU-03 usa Authentication, Firestore y Storage Emulator en `demo-huellitas-chalco`; las lecturas públicas funcionan sin sesión. No hay cambios en las reglas ni nuevos permisos de escritura. Se conserva Spark y la instalación `.local` separada de la app real.

La ubicación mediante Google Maps corresponde a HU-18; los requisitos y la solicitud se conectan en HU-04/HU-05. La ficha normal no presenta un mapa simulado como si fuera una integración real. Las variantes del muestrario conservan sus ejemplos de diseño.

## Pruebas

Verificación completada: **60 pruebas unitarias y 15 Android**, compilación normal/local y Android Lint sin errores. Las reglas son las ya verificadas en las 59 pruebas de HU-02; HU-03 no las modifica.

Las pruebas unitarias comprueban el ID seleccionado, espera de su refugio, ausencia de información inventada, cambios de salud y descripción, retirada por adopción, errores, tiempo de espera y reintento. Las pruebas Android consultan refugios activos, inactivos, inexistentes y de otro municipio con el SDK real; recorren la galería, la ficha y el regreso desde el refugio. También se revisan el catálogo, el registro/edición de perritos y el acceso de usuarios.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Para la prueba local en tableta, usar el arranque y la compilación de [dog-publication.md](dog-publication.md), preparar `seed:local` y `seed:catalog`, e instalar el APK local y el APK de pruebas. Los recorridos están en `FirebaseCatalogSdkTest`, `CatalogFlowTest`, `DogRegistrationFlowTest` y `AuthFlowTest`.
