# HU-11 — Descripciones con Gemini

Estado: código e interacción preparados y comprobados localmente. Falta habilitar Gemini Developer API, registrar la instalación de desarrollo en App Check y ejecutar la prueba real. HU-11 permanece pendiente en el checklist.

## Uso

En Registrar perrito, completar nombre, edad, tamaño y sexo y pulsar Redactar con Gemini. Generar propuesta es una acción explícita; abrir el asistente no llama al servicio. Se puede editar el texto, usarlo o descartarlo. Solo Usar propuesta copia el texto a la descripción del borrador. Después sigue la revisión y publicación habitual de HU-08. Gemini no escribe perritos en Firestore ni publica automáticamente.

Un error de conexión, tiempo agotado, falta de configuración o propuesta rechazada permite volver a escribir manualmente. Se conserva la descripción original. Hay un solo pedido en curso; cerrar el asistente cancela su espera y evita que un resultado tardío reabra la propuesta.

## Datos y controles

El servicio recibe nombre, edad canónica en meses, tamaño, sexo y notas de la descripción capturada. No recibe imágenes, perfiles, solicitudes, domicilios, membresías ni los campos de salud.

La instrucción del modelo limita la propuesta a los datos proporcionados y trata las notas como contenido, nunca como órdenes. Se comprueban longitud, nombre, edad, tamaño y sexo. Se rechazan términos médicos, enlaces, números no proporcionados y diversas características habituales si no aparecen en las notas. Estos filtros no son una verificación semántica completa: el responsable debe revisar que el texto no agregue hechos antes de usarlo. Sus ediciones manuales se validan como la descripción del formulario.

## Firebase y plan escolar

Se usa Firebase AI Logic con Gemini Developer API y el modelo configurable `gemini-3.5-flash-lite`, disponible sin facturación según la lista oficial consultada. No se habilita Vertex/Agent Platform, Blaze ni una cuenta de facturación.

AI Logic necesita el servicio real: no se presenta una respuesta fija de un emulador como si fuera Gemini. Auth, Firestore, Storage y Functions conservan la configuración local elegida. Una instancia Firebase separada, `huellitas-gemini`, usa únicamente la configuración real de `app/google-services.json` para las acciones explícitas de IA. Esa configuración se lee localmente al compilar y no se incluye en Git. La clave privada de Gemini que administra Firebase no se copia a la app.

App Check usa el proveedor de depuración en builds debug y Play Integrity en release. La autorización de la instalación de desarrollo está pendiente. No se deben publicar tokens de depuración; las preferencias de builds debug están excluidas del respaldo y transferencia. El modelo puede cambiarse con `-PgeminiModel=...` después de comprobar que siga disponible en el proveedor elegido.

## Activación pendiente

La consola de `huellitas-chalco` muestra el botón Habilita las APIs y exige aceptar las Condiciones del Servicio Adicionales de Gemini y sus Políticas de Uso Adicionales. Son condiciones nuevas respecto de la aceptación previa de Firebase. La activación y el registro de App Check requieren autorización del propietario antes de continuar.

Después de autorizar: habilitar solo Gemini Developer API sin facturación, mantener App Check activo, registrar el token privado de esta instalación de pruebas, completar el asistente sin servicios opcionales de pago y ejecutar una petición con datos ficticios del perrito. No se cambian las membresías ni las reglas de Firestore/Storage del proyecto real.

## Pruebas

109 pruebas unitarias totales sin fallos: captura de datos, rechazo de información no sustentada, falta de configuración, tiempo agotado, cancelación, doble envío, edición y descarte. La interfaz se comprobó con un generador controlado: error, retorno al texto original, propuesta editable, descarte y aceptación sin llamar a publicar. Pasaron también las regresiones de alta y los 16 destinos privados (18 pruebas Android); la prueba de IA se repitió después de reforzar las validaciones y pasó.

La prueba real es independiente y se ejecuta únicamente después de activar y autorizar el servicio:

```sh
adb -s R5GYB24P92J shell am instrument -w \
  -e class com.example.huellitaschalco.feature.shelter.description.GeminiLiveSdkTest \
  -e hu11RealAI true \
  com.example.huellitaschalco.local.test/androidx.test.runner.AndroidJUnitRunner
```

La prueba manda solo una ficha ficticia y exige una propuesta válida. No escribe ni publica perritos. No se cuenta como superada hasta recibir una respuesta real del modelo.

Referencias: [inicio de AI Logic](https://firebase.google.com/docs/ai-logic/get-started?platform=android), [modelos admitidos](https://firebase.google.com/docs/ai-logic/models), [precios y Spark](https://firebase.google.com/docs/ai-logic/pricing), [condiciones de Gemini](https://ai.google.dev/gemini-api/terms), [políticas de uso](https://ai.google.dev/gemini-api/docs/usage-policies).

Compilaciones normal/local, 109 pruebas unitarias y Android Lint finales correctos. No se ha ejecutado la prueba GeminiLiveSdkTest ni se han aceptado las condiciones adicionales: la autorización está pendiente.
