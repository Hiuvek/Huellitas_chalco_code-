# Firebase Spark — Huellitas Chalco

La app normal usa el proyecto `huellitas-chalco` mediante internet. No requiere emuladores ni una computadora encendida. No se activa Blaze ni se vincula facturación.

## Servicios

- Authentication: cuentas y sesiones.
- Firestore: catálogo, salud, requisitos, solicitudes, evaluaciones, entregas, revisiones, seguimientos e imágenes.
- Room y archivos privados: borradores y caché pública sin conexión; no sincronizan información privada entre cuentas.
- Gemini mediante Firebase AI Logic: conserva la configuración existente y su cuota gratuita. La descripción también se puede escribir manualmente.

## Fotografías

Una fotografía por perrito, comprimida a JPEG de hasta 100 KiB y dimensiones máximas de 640 píxeles al importar. El seguimiento admite una fotografía privada opcional con el mismo límite. Se elimina la información EXIF al convertir.

`imagenes/{sha256(ruta)}` contiene `ruta`, `refugioId`, `perritoId` o `revisionId`, y `datos` en Base64. Los documentos se descargan individualmente; no forman parte del listado del catálogo. `datos` no se indexa. La caché pública existente evita descargar de nuevo una portada ya disponible.

Las reglas limitan tamaño, formato Base64 y rutas, exigen membresía y hacen inmutable el contenido. Android comprueba que la imagen sea un JPEG decodificable antes de subirla; las reglas no ejecutan un decodificador de imágenes. Las fotografías de seguimientos solo son visibles para responsables del refugio. Al quedar adoptado el perrito, su foto permanece accesible al adoptante correspondiente y al refugio.

Las fotografías antiguas alojadas únicamente en Storage deben importarse a `imagenes`; no se migran automáticamente. No se copian datos de emuladores al proyecto real.

## Operaciones protegidas

`SparkOperations` ejecuta transacciones del SDK Android. Las reglas de Firestore verifican permisos y estados, incluso si se modifica la app:

- Entrega: crea `adopciones/{perritoId}`, cambia el perrito a Adoptado y completa su control en una sola transacción. El identificador único impide duplicar adopciones.
- Solicitudes restantes: cada cierre se guarda junto a su historial. Se realizan después de confirmar la entrega para respetar los límites de reglas. Si se interrumpe la conexión, se puede usar «Completar cierre de solicitudes» en la entrega guardada para terminar esos cierres; la adopción ya confirmada se conserva.
- Revisiones: requieren una adopción válida y una fecha posterior o igual a la entrega.
- Seguimiento: crea un resultado inmutable, completa su revisión y, si corresponde, programa la siguiente revisión en una misma transacción.
- Los reintentos conservan identificadores y verifican los datos ya guardados. Las transacciones necesitan conexión y no dejan operaciones de entrega pendientes en una cola local.

No hay procesos programados ni cierres en segundo plano cuando todos los dispositivos están desconectados. Los archivos de `functions/` y `storage.rules` se conservan como referencia anterior y no están incluidos en `firebase.json`.

## Uso y cuotas

Spark aplica sus límites gratuitos. El almacenamiento de imágenes consume la cuota de Firestore y Base64 aumenta aproximadamente un tercio el tamaño. Al alcanzar una cuota, el servicio puede rechazar operaciones; no se promete uso ilimitado. Revisa el consumo y elimina desde administración los documentos de imágenes huérfanas cuando se reemplacen fotografías o abandonen cargas.

## Compilar y publicar

La configuración real `app/google-services.json` permanece fuera de Git.

```bash
npm --prefix tools/firebase ci
node tools/cloud/status.mjs
tools/cloud/deploy.sh
tools/cloud/build-apk.sh
```

Solo se publican reglas e índices de Firestore. Resultado: `build/cloud/Huellitas-Chalco-Spark.apk`, una compilación debug para presentación privada. Para actualizar una instalación existente se debe conservar su firma; el script acepta `HUELLITAS_GRADLE_INIT` con un archivo local de firma. No es una publicación en Google Play.

Para pruebas aisladas, `npm --prefix tools/firebase run start` inicia únicamente Auth y Firestore del proyecto demo; el APK `.local` sigue disponible y requiere USB/emuladores. Véase [guía local](demo-un-dispositivo.md).

Resultados de verificación: [pruebas de ambos roles](pruebas-spark.md). El refugio de demostración real tiene ahora los requisitos y pasos escolares de ejemplo; no son políticas verificadas de un refugio real.

## Catálogo escolar de prueba

El 22 de noviembre se agregaron ocho fichas de demostración al proyecto real: Luna, Max, Nala, Rocky, Kira, Toby, Maya y Bruno. Todas están disponibles, pertenecen al refugio de demostración y tienen edades, tamaños y sexos variados. Su salud figura como «Sin información». Cada descripción aclara que los datos son ficticios; las fotografías son las imágenes de ejemplo existentes en la app, comprimidas entre 10 y 20 KiB.

`node tools/cloud/seed-demo.mjs` muestra la preparación y `--write` crea las fichas, imágenes y controles de adopción. El script exige el refugio escolar activo y utiliza precondiciones de creación; no reemplaza documentos existentes ni restablece adopciones que el usuario ya haya probado. Se verificó sin sesión que las ocho fichas y sus imágenes son legibles desde Firebase real. No requiere actualizar el APK.
