# Checklist — Huellitas Chalco

Actualización: 22 de noviembre de 2026. La app normal está adaptada a Firebase Spark: Authentication, Firestore e imágenes pequeñas en documentos separados. No utiliza Cloud Storage ni Cloud Functions. Room conserva borradores y caché local.

## Historias de usuario

| Historia | Funcionalidad | Estado de implementación |
| --- | --- | --- |
| HU-01 | Registro e inicio de sesión | [x] Firebase Authentication |
| HU-02 | Catálogo de perritos de Chalco | [x] Firestore, búsqueda y filtros |
| HU-03 | Fotografía, edad, tamaño, sexo y descripción | [x] Ficha del registro seleccionado |
| HU-04 | Requisitos y pasos de adopción | [x] Configuración pública por refugio; requiere datos completos |
| HU-05 | Solicitud de adopción | [x] Formulario validado, envío e historial atómicos |
| HU-06 | Mis solicitudes | [x] Consulta privada y estados |
| HU-07 | Acceso del responsable | [x] Membresías y rutas protegidas |
| HU-08 | Registrar perritos | [x] Datos y una foto comprimida en Firestore |
| HU-09 | Vacunación, desparasitación y esterilización | [x] Captura y edición |
| HU-10 | Consultar y editar mis perritos | [x] Datos, reemplazo de foto y detección de conflictos |
| HU-11 | Descripción con Gemini | [x] Propuesta editable, revisión humana y alternativa manual |
| HU-12 | Solicitudes recibidas | [x] Bandeja privada por refugio |
| HU-13 | Evaluación de solicitudes | [x] Estados, motivo e historial; aprobación única |
| HU-14 | Confirmar entrega | [x] Transacción de Firestore, adopción única y cierre recuperable de pendientes |
| HU-15 | Programar revisiones | [x] Agenda vinculada a la adopción |
| HU-16 | Seguimiento posterior | [x] Resultado, foto privada opcional y próxima revisión |
| HU-17 | Uso local con Room | [x] Borradores por cuenta y caché pública |
| HU-18 | Ubicación del refugio | [x] Zona pública aproximada y apertura de Google Maps |

## Adaptación a Spark

- [x] Sustituir las tres operaciones de Cloud Functions por transacciones del SDK Android.
- [x] Validar permisos, estados, fechas y operaciones relacionadas en reglas de Firestore.
- [x] Fotografías JPEG de hasta 100 KiB, una por perrito y una opcional por seguimiento.
- [x] Descargar imágenes individualmente y excluir su contenido de los índices.
- [x] Conservar la separación de datos entre cuentas, refugios y roles.
- [x] Mantener emuladores de Auth/Firestore solo para pruebas aisladas.
- [x] Publicar reglas e índices en el proyecto real, sin activar facturación.
- [x] Probar los recorridos de ambos roles en la tableta y corregir los problemas encontrados.
- [x] Comprobar fotografías con clientes independientes contra el proyecto real.
- [x] Instalar y entregar el APK normal actualizado, versión 1.1.

No se ha comprobado el uso simultáneo de dos dispositivos físicos. El modo normal usa internet; el APK `.local` continúa siendo una instalación separada para pruebas con emuladores. Gemini conserva su propia cuota gratuita y configuración de App Check.

Detalles técnicos y limitaciones: [modo Spark](docs/spark.md). Resultados anteriores a esta adaptación: [checklist histórico](docs/checklist-anterior-a-spark.md).

Verificación: **114 pruebas unitarias, 85 de reglas y 63 comprobaciones en tableta correctas**, incluida una generación real con Gemini. [Reporte de pruebas y limitaciones](docs/pruebas-spark.md).

- [x] Catálogo real preparado con ocho perritos ficticios, fotografías y controles para probar solicitudes y adopciones.
