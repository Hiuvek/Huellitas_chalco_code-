# Adopción completa en Firebase real

Fecha: 22 de noviembre de 2026. App normal 1.2 en la tableta Samsung. Proyecto `huellitas-chalco`, Spark sin facturación e índices READY al iniciar.

## Resultado

`SparkCloudAdoptionTest` pasó: 1 prueba integrada con los repositorios y SDK Android reales. No usa credenciales administrativas para ejecutar las operaciones. El responsable utiliza su sesión existente; el adoptante utiliza otra instancia Firebase con sesión y caché independientes. Ninguna operación se dirige a emuladores.

- Publicación de una ficha nueva de demostración con fotografía pequeña.
- Creación de una cuenta ficticia de adoptante y envío de solicitud.
- Lectura de la solicitud desde el cliente del responsable y reintento sin duplicación.
- Paso a revisión y aprobación, comprobada desde el cliente del adoptante.
- Registro de entrega simulada y reintento conservando una sola adopción.
- Programación de revisión y registro de resultado con fotografía privada.
- Reintento del seguimiento sin duplicación y descarga de la foto por el responsable.
- Rechazo de la lectura del seguimiento por el adoptante.
- Conservación de la sesión original del responsable.

## Evidencia conservada

| Registro | Identificador |
| --- | --- |
| Perrito ficticio | `spark-e2e-20261122-01` — Prueba adopción nube |
| Solicitud | `507899cc-6f67-4bf2-af73-bddd2c7fb005` |
| Adopción | `spark-e2e-20261122-01` |
| Revisión y seguimiento | `d33605d7-28df-4a66-91cc-ac6fae7f3e36` |

Los registros permanecen como evidencia escolar en el refugio de demostración. La descripción y observaciones aclaran que son ficticios: no se entregó un animal ni se firmó una carta real. Los ocho perritos anteriores del catálogo se conservaron. La cuenta ficticia usa un correo del dominio reservado `example.test`; no se envía correo ni se guardan contraseñas en el repositorio.

## Comprobación visual

Desde la sesión conservada del responsable se abrió Seguimientos → Historial → Prueba adopción nube → Ver seguimiento. La pantalla mostró la revisión realizada, fecha, adaptación Buena, observaciones de demostración, folio y fotografía privada. La imagen es un JPEG verde generado para la prueba técnica, no la fotografía de un animal real. Se dejó esta pantalla abierta en la tableta.

Captura local: `work/spark-adoption-result.png`, fuera de Git. No se modificó el APK de la aplicación; solo se añadió una prueba de instrumentación.

## Repetir de forma explícita

Compilar e instalar el APK de instrumentación normal con la misma firma de la app. Se requiere la sesión de un responsable activo del refugio de demostración en la app normal. Usar un identificador nuevo; la prueba rechaza uno existente antes de escribir.

```bash
adb shell am instrument -w \
  -e class com.example.huellitaschalco.core.firebase.SparkCloudAdoptionTest \
  -e sparkAdoption true \
  -e sparkDogId spark-e2e-IDENTIFICADOR-NUEVO \
  com.example.huellitaschalco.test/androidx.test.runner.AndroidJUnitRunner
```

Sustituir el identificador por letras minúsculas, números o guiones. La prueba se omite si no se activa expresamente o si la compilación apunta a emuladores. Cada ejecución crea una cuenta ficticia y registros nuevos; no restablece ni borra pruebas anteriores.

## Alcance

La comprobación automatizada recorre los repositorios Android contra el servidor real. Complementa las pruebas de interfaz con emuladores; no representa una ejecución manual de todos los formularios ni simultaneidad en dos dispositivos físicos. No se prueba Gemini en este recorrido.

Registros locales: `work/spark-adoption-build.log`, `work/spark-adoption-cloud.log` y `work/spark-adoption-stages.log`, en el directorio de trabajo de esta revisión fuera de Git.
