# Demostración escolar con un dispositivo

Esta modalidad mantiene Spark y no vincula facturación. Auth, Firestore, Storage y Functions se ejecutan en la computadora mediante emuladores. La tableta usa la app **Huellitas Chalco local** (`com.example.huellitaschalco.local`). La app normal conserva sus datos y su conexión al proyecto real.

Gemini usa el servicio real de `huellitas-chalco`, con internet y el token privado de esta instalación autorizado en App Check. No tiene emulador. Si se alcanza su cuota gratuita, se puede escribir la descripción manualmente; no hace falta activar Blaze.

## Preparación

1. Desde la raíz del repositorio, inicia los emuladores y deja la terminal abierta:

   ```bash
   npm --prefix tools/firebase run start
   ```

2. Para una demostración nueva, prepara las cuentas y fichas ficticias desde otra terminal:

   ```bash
   npm --prefix tools/firebase run seed:local
   npm --prefix tools/firebase run seed:catalog
   npm --prefix tools/firebase run seed:requests
   npm --prefix tools/firebase run seed:adoptions
   ```

   `seed:requests` restablece las solicitudes A/B de muestra y sus adopciones, revisiones y seguimientos asociados. Úsalo antes del recorrido, no mientras estás mostrando esas fichas. Estos comandos apuntan exclusivamente a `demo-huellitas-chalco`. Para conservar otra sesión de emuladores, expórtala antes con `tools/firebase/node_modules/.bin/firebase emulators:export RUTA_NUEVA --project demo-huellitas-chalco --config firebase.json`.

3. Conecta la tableta por USB, autoriza la depuración en el dispositivo y ejecuta:

   ```bash
   tools/demo/connect-device.sh
   ```

   Repite este paso después de reconectar el USB. Si hay varios dispositivos, indica su serie como argumento. El script conecta los cuatro servicios a través de USB; no abre puertos a la red.

4. Instala el APK de demostración `.local`. No desinstales ni borres los datos de la app normal. Si Android rechaza una actualización por una firma diferente, conserva los datos y recompila con la firma original.

   Para generar el APK desde el código:

   ```bash
   tools/demo/build-apk.sh
   ```

   Resultado: `build/demo/Huellitas-Chalco-demo.apk`. La variable opcional `HUELLITAS_GRADLE_INIT` permite usar una configuración local de firma existente sin guardarla en Git. El APK es debug y de uso privado para la demostración; no es una versión de distribución pública.

## Cuentas de muestra

Todas son ficticias y pertenecen únicamente al emulador. Contraseña común: `PruebaHU07!`.

| Cuenta | Uso |
| --- | --- |
| hu07-a@example.test | Responsable del refugio A |
| hu07-b@example.test | Responsable del refugio B; comprobar separación de datos |
| hu07-adopter@example.test | Adoptante |

## Recorrido en la misma tableta

- [ ] Entrar como responsable A desde Acceso refugios.
- [ ] Registrar un perrito nuevo: fotografía, nombre, edad, tamaño, sexo y descripción.
- [ ] Comprobar los tres campos de salud y revisar la vista previa antes de publicar.
- [ ] Abrir Gemini: generar una propuesta, editarla o descartarla; comprobar que solo se publica al confirmar el formulario.
- [ ] Editar el perrito y comprobar sus cambios en la ficha.
- [ ] Cerrar sesión y entrar como adoptante.
- [ ] Buscar el perrito, consultar fotos, salud, requisitos e información pública del refugio.
- [ ] Abrir la zona pública en Google Maps; requiere internet y una app compatible o navegador.
- [ ] Completar y enviar una solicitud; verificar su aparición en Mis solicitudes.
- [ ] Cerrar sesión y volver como responsable A.
- [ ] Abrir la solicitud recibida, pasarla a revisión y aprobarla.
- [ ] Registrar la entrega con fecha y carta; comprobar el estado de adopción.
- [ ] Programar una revisión y guardar su resultado, con foto opcional y siguiente revisión si corresponde.
- [ ] Entrar como responsable B y comprobar que no se muestran los datos privados del refugio A.

El cambio de cuenta permite comprobar ambos roles con un solo dispositivo. La sincronización simultánea entre dos celulares sigue pendiente.

## Room y falta de conexión

- [ ] Consultar el catálogo y una ficha mientras los emuladores están accesibles.
- [ ] Crear un borrador de solicitud o alta, salir de la pantalla y comprobar su restauración al volver.
- [ ] Cerrar y abrir la app y verificar nuevamente el borrador, sin borrar datos.
- [ ] Interrumpir la conexión USB con los emuladores y comprobar el aviso de copia guardada en el catálogo.
- [ ] Buscar y consultar la ficha y requisitos guardados; una fotografía no consultada antes puede no estar disponible.
- [ ] Comprobar que no se confirma un envío sin conexión ni se pierde el texto del borrador.
- [ ] Reconectar USB, ejecutar `tools/demo/connect-device.sh` y reintentar la actualización.

El acceso administrativo necesita validar membresía con el servidor. Room conserva datos y borradores; no concede acceso a un refugio sin esa validación.

## Si Gemini muestra un error

| Aviso | Qué hacer |
| --- | --- |
| Sin conexión | Revisar internet y reintentar; los emuladores no sustituyen la conexión de Gemini. |
| Instalación no autorizada | Autorizar su token privado en App Check. La app normal y `.local` tienen tokens distintos. |
| Límite de uso | Esperar y reintentar más tarde o escribir manualmente. |
| Configuración no disponible | Revisar la configuración real local y que Gemini Developer API siga habilitada. |
| Tiempo agotado o servicio no disponible | Reintentar; se conserva la descripción y se permite escribir manualmente. |
| Propuesta rechazada | Revisar las notas o escribir manualmente; no se debe inventar información del perrito. |

Los tokens privados de depuración no se guardan en Git ni se incluyen como constantes en el APK. El APK contiene la configuración habitual del cliente Firebase, no la clave privada de Gemini. El token de depuración se genera y guarda privadamente en cada instalación.

## Pendiente para otra sesión

- [ ] Sincronización real y simultánea con dos dispositivos.
- [ ] Funcionamiento completo desde redes distintas sin computadora.
- [ ] Configuración y prueba de Play Integrity para una versión release.

Estas tareas no se cuentan como verificadas por las pruebas de un solo dispositivo.

## Resultados confirmados y cierre del 22 de noviembre de 2026

Las ejecuciones en la tableta finalizaron el 21 de noviembre; este cierre se documentó el 22. No se presenta como una nueva prueba con el dispositivo desconectado.

| Comprobación | Resultado |
| --- | --- |
| Compilación normal y local; Android Lint | Sin errores |
| Pruebas unitarias | 113 correctas |
| Reglas de Firestore y Storage | 89 correctas |
| Functions de entrega, programación y seguimiento | 17 correctas |
| Room, catálogo sin conexión, asistente IA, accesos privados, Maps y formularios | 39 pruebas Android correctas |
| Recepción y evaluación de solicitudes | 2 pruebas Android correctas |
| Confirmación de entrega | 1 prueba Android correcta |
| Programación y registro de seguimiento | 2 pruebas Android correctas |
| Publicación, salud, edición y solicitudes por SDK | 24 pruebas Android correctas |
| Gemini real con App Check en app normal actualizada | 1 prueba Android correcta |

Son 69 comprobaciones Android correctas en los grupos finales, además de las pruebas unitarias, de reglas y de funciones. La prueba de evaluación se repitió tras retirar una operación innecesaria de cerrar el teclado del diálogo; su ejecución final pasó. Los errores específicos de Gemini se comprobaron con pruebas controladas, conservando la propuesta anterior y evitando mostrar respuestas privadas del servidor.

El APK de demostración se generó, se verificaron su paquete `.local` y su firma, y se instaló como actualización conservando datos. El script de construcción se ejecutó correctamente y el de conexión USB se comprobó con la tableta. También se actualizó la app normal para incorporar los mensajes de Gemini. No se habilitó facturación ni se desplegaron servicios adicionales en la nube.

La base de Room se verificó mediante cierre y reapertura de archivos reales y recreación de ViewModels, junto con el recorrido de interfaz sin conexión de Firestore. No se afirma haber probado todas las interrupciones posibles del sistema operativo.

La prueba entre dos dispositivos y el uso completo desde redes independientes siguen pendientes. Al terminar esta sesión ya no había una tableta conectada ni emuladores activos: para una nueva demostración hay que seguir los pasos de Preparación.
