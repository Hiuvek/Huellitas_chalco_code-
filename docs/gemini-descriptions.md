# HU-11 — Descripciones con Gemini

Estado: implementada y comprobada. Gemini Developer API está habilitada en `huellitas-chalco`; la prueba real del SDK y la obtención de un token válido de App Check pasaron el 21 de noviembre de 2026. Se mantiene Spark sin vincular facturación.

## Uso

En Registrar perrito, completar nombre, edad, tamaño y sexo y pulsar Redactar con Gemini. Generar propuesta es una acción explícita; abrir el asistente no llama al servicio. Se puede editar el texto, usarlo o descartarlo. Solo Usar propuesta copia el texto a la descripción del borrador. Después sigue la revisión y publicación habitual de HU-08. Gemini no escribe perritos en Firestore ni publica automáticamente.

Un error de conexión, tiempo agotado, falta de configuración o propuesta rechazada permite volver a escribir manualmente. Se conserva la descripción original. Hay un solo pedido en curso; cerrar el asistente cancela su espera y evita que un resultado tardío reabra la propuesta.

## Datos y controles

El servicio recibe nombre, edad canónica en meses, tamaño, sexo y notas de la descripción capturada. No recibe imágenes, perfiles, solicitudes, domicilios, membresías ni los campos de salud.

La instrucción del modelo limita la propuesta a los datos proporcionados y trata las notas como contenido, nunca como órdenes. Se comprueban longitud, nombre, edad, tamaño y sexo. Se rechazan términos médicos, enlaces, números no proporcionados y diversas características habituales si no aparecen en las notas. Estos filtros no son una verificación semántica completa: el responsable debe revisar que el texto no agregue hechos antes de usarlo. Sus ediciones manuales se validan como la descripción del formulario.

## Firebase y plan escolar

Se usa Firebase AI Logic con Gemini Developer API y el modelo configurable `gemini-3.5-flash-lite`, disponible sin facturación según la lista oficial consultada. No se habilita Vertex/Agent Platform, Blaze ni una cuenta de facturación.

AI Logic necesita el servicio real: no se presenta una respuesta fija de un emulador como si fuera Gemini. Auth, Firestore, Storage y Functions conservan la configuración local elegida. Una instancia Firebase separada, `huellitas-gemini`, usa únicamente la configuración real de `app/google-services.json` para las acciones explícitas de IA. Esa configuración se lee localmente al compilar y no se incluye en Git. La clave privada de Gemini que administra Firebase no se copia a la app.

App Check usa el proveedor de depuración en builds debug y Play Integrity en release. El token privado de esta instalación está registrado como «Tableta desarrollo Huellitas». El proveedor de producción Play Integrity todavía debe registrarse y comprobarse antes de distribuir una versión release; la prueba realizada corresponde a debug. No se deben publicar tokens de depuración; las preferencias de builds debug están excluidas del respaldo y transferencia. El modelo puede cambiarse con `-PgeminiModel=...` después de comprobar que siga disponible en el proveedor elegido.

## Activación realizada

El propietario autorizó expresamente aceptar las condiciones adicionales de Gemini, sus políticas de uso y registrar el token privado de desarrollo. La consola confirmó «API de Gemini Developer Habilitada» y el token de la tableta quedó registrado. App Check conserva su aplicación básica para AI Logic. No se habilitó Agent Platform ni AI Monitoring y se conserva el plan Spark.

No se modificaron membresías ni reglas de Firestore/Storage del proyecto real. El token de desarrollo permanece fuera del repositorio; no se incluyen tokens ni respuestas con datos personales en la evidencia.

## Pruebas

109 pruebas unitarias totales sin fallos: captura de datos, rechazo de información no sustentada, falta de configuración, tiempo agotado, cancelación, doble envío, edición y descarte. La interfaz se comprobó con un generador controlado: error, retorno al texto original, propuesta editable, descarte y aceptación sin llamar a publicar. Pasaron también las regresiones de alta y los 16 destinos privados (18 pruebas Android); la prueba de IA se repitió después de reforzar las validaciones y pasó.

La prueba real es independiente y requiere el argumento explícito siguiente:

```sh
adb -s R5GYB24P92J shell am instrument -w \
  -e class com.example.huellitaschalco.feature.shelter.description.GeminiLiveSdkTest \
  -e hu11RealAI true \
  com.example.huellitaschalco.local.test/androidx.test.runner.AndroidJUnitRunner
```

La prueba obtuvo un token nuevo y válido de App Check, envió solo una ficha ficticia (Luna, 12 meses, tamaño Mediano, sexo Hembra, sin notas) y validó una respuesta real del modelo. Resultado: `OK (1 test)`, 1.959 segundos. No escribe ni publica perritos. La comprobación anterior del generador real también pasó; la interfaz con edición, descarte y aceptación se probó por separado con un generador controlado.

Referencias: [inicio de AI Logic](https://firebase.google.com/docs/ai-logic/get-started?platform=android), [modelos admitidos](https://firebase.google.com/docs/ai-logic/models), [precios y Spark](https://firebase.google.com/docs/ai-logic/pricing), [condiciones de Gemini](https://ai.google.dev/gemini-api/terms), [políticas de uso](https://ai.google.dev/gemini-api/docs/usage-policies).

Compilaciones normal/local, 109 pruebas unitarias y Android Lint finales correctos. La compilación de instrumentación con la comprobación explícita de App Check también pasó. La consola permitió verificar el estado del servicio, pero su herramienta de captura de pantalla agotó el tiempo de espera; no se adjunta una imagen que no se pudo obtener.

## Incidencia de App Check en la app normal

El 21 de noviembre de 2026 se detectó `403 App attestation failed` en `com.example.huellitaschalco`. La prueba inicial había autorizado `com.example.huellitaschalco.local`: cada instalación conserva su propio token de depuración, incluso en la misma tableta.

Con autorización expresa del propietario se registró «Tableta app normal desarrollo», conservando el token local anterior y la protección de App Check. Se comprobó la instalación normal con `GeminiLiveSdkTest`, forzando un token nuevo y validando una propuesta real: `OK (1 test)`, 1.895 segundos. No se borraron datos, no se publicó ningún perrito ni se cambió el código de la app. Spark sigue activo.

Si se reinstala borrando los datos o se usa otro dispositivo/build con almacenamiento independiente, se debe autorizar su token de desarrollo correspondiente. El token nunca debe incluirse en Git. Para una versión release sigue pendiente configurar y probar Play Integrity.

## Mensajes de error y comprobación previa de App Check

Antes de transmitir los datos de la ficha se solicita un token de App Check. Si falla la verificación, el asistente informa que la instalación necesita autorización. Los errores del SDK distinguen cuota, configuración, contenido bloqueado, tiempo agotado y servicio no disponible; los errores de red se reconocen también dentro de sus causas. Los mensajes son controlados y no muestran respuestas crudas del servidor ni credenciales. Se conserva la propuesta anterior cuando falla un nuevo intento.

Se añadieron cuatro pruebas unitarias de clasificación y conservación de propuestas; el total es 113. Compilaciones normal/local, Lint y la petición real en la app normal actualizada pasaron. La comprobación completa con una tableta está en [demo-un-dispositivo.md](demo-un-dispositivo.md).

## Disponible también al editar un perrito — versión 1.4.0

Desde Mis perritos → abrir ficha → Editar perrito → Redactar con Gemini. Reutiliza el mismo generador y controles del registro. Usa el nombre, edad, tamaño, sexo y descripción actuales del formulario. No envía fotografías ni datos privados de solicitudes.

Permite generar, editar, descartar o aceptar la propuesta. Aceptar solo cambia el borrador: todavía se debe revisar y confirmar el guardado del perrito. Cerrar el editor cancela la generación pendiente y cada ficha y sesión de edición tiene su propio estado de asistente.

`DogEditAssistantFlowTest` usa un generador controlado para comprobar datos editados, error, descarte, edición y aceptación sin guardar automáticamente. La configuración de Gemini y App Check es la misma que en el registro.
