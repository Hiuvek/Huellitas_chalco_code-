# Commits por historia de usuario

Por indicación posterior del propietario, se conserva únicamente `main`. Los cambios actuales se registran en esa rama; solo se creará otra si el propietario lo solicita. Los cambios se guardan en commits pequeños y completos, separados por su propósito, para que el historial muestre cómo se construyó la historia.

Formato: `tipo: HU-XX descripción concreta del cambio`.

| Tipo | Cuándo usarlo | Ejemplo para HU-08 |
| --- | --- | --- |
| `chore:` | Dependencias, herramientas y configuración | `chore: HU-08 configurar Firebase Storage` |
| `feat:` | Una capacidad nueva de la aplicación | `feat: HU-08 validar datos del perrito` |
| `feat:` | Otro paso funcional que pueda revisarse por separado | `feat: HU-08 subir fotografías y publicar perritos` |
| `fix:` | Corregir un problema encontrado | `fix: HU-08 conservar el formulario si falla la carga` |
| `test:` | Pruebas de comportamiento, permisos o errores | `test: HU-08 comprobar publicación y aislamiento entre refugios` |
| `docs:` | Alcance, configuración, evidencia y checklist | `docs: HU-08 documentar pruebas y actualizar checklist` |
| `refactor:` | Reorganizar código conservando su comportamiento | `refactor: HU-08 separar la carga de fotografías del formulario` |

Los ejemplos describen el formato; HU-08 está implementada. Solo se utilizan los tipos que correspondan a cambios reales. No se crean commits vacíos ni correcciones artificiales para completar la lista. Los cambios estrechamente relacionados pueden compartir un commit.

## Flujo de trabajo

1. Revisar el checklist y los criterios de la historia; conservar los cambios locales ajenos a ella.
2. Implementar pasos coherentes y registrar cada uno con su identificador de historia y el tipo correspondiente. Cada commit debe dejar el proyecto en un estado válido.
3. Ejecutar las comprobaciones adecuadas a cada cambio. Los cambios de documentación no requieren recompilar la app.
4. Registrar las pruebas y limitaciones en la documentación de la historia. Marcarla como completada solo cuando cumpla sus criterios.
5. Subir los commits desde esta computadora a la rama de GitHub y verificar que el último commit exista en el remoto. La autorización del usuario para subir cada historia se mantiene para las siguientes entregas.
6. Mantener únicamente `main`, según la instrucción vigente del propietario. Publicar una rama no implica que ya esté integrada.

Las reglas y configuración de Firebase necesarias para reproducir el proyecto se versionan. Las credenciales administrativas, contraseñas reales, archivos locales excluidos de Git, dependencias descargadas y resultados de compilación se mantienen fuera de los commits.

## Historial existente

HU-01 se publicó como `feat: H1`. La implementación, pruebas y documentación iniciales de HU-07 se publicaron juntas en `d7fc974` (`feat: HU-07 acceso autorizado de refugios`). Ese historial se conserva; el desglose por propósito se aplica a los cambios siguientes.
