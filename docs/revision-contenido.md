# Revisión de contenido — versión 1.5.0

Se mantuvieron los campos, diseño, navegación, validaciones, permisos y estructura de la base de datos. Los cambios del código de la app se limitan a textos visibles y a retirar los bloques que mostraban avisos sobre el origen del contenido. Las herramientas de carga y verificación están fuera de la app.

## Refugio

| Campo existente | Contenido actualizado |
| --- | --- |
| Nombre | Huellitas del Valle |
| Zona pública | Centro de Chalco |
| Horario | Lunes a sábado, de 10:00 a 17:00 h. Visitas con cita previa. |
| Requisitos | Mayoría de edad, hogar seguro, acuerdo familiar, tiempo y recursos, entrevista, cuidados y seguimiento. |
| Pasos | Conocer la ficha, enviar solicitud, entrevista, convivencia y entrega, seguimiento de adaptación. |

Por elección del usuario no se agregaron descripción del refugio, misión, historia, teléfono, correo, redes ni una dirección exacta: esos campos no existen. Google Maps conserva la zona pública aproximada. No se publicaron contactos de terceros ni ubicaciones privadas.

## Perritos

Las 12 fichas anteriores se reescribieron en los campos existentes de nombre y descripción. Se normalizaron Blacky y Kyle; los dos registros retirados que conservaban nombres técnicos ahora se llaman Milo y Rufus. No se reactivaron registros retirados ni se cambiaron adopciones, edades, tamaños, sexo, fotos o estados de salud anteriores.

La descripción contiene raza o mezcla, historia de llegada, personalidad, energía, convivencia con niños/perros/gatos, salud y hogar ideal. La edad, sexo y tamaño siguen en sus campos actuales. Cuando la salud anterior no está registrada, la descripción conserva «Sin información»; no se inventaron vacunaciones o procedimientos para esos registros.

| Nuevos | Edad | Tamaño | Sexo | Característica |
| --- | --- | --- | --- | --- |
| Chispa | 4 meses | Chico | Hembra | Cachorra mestiza de terrier, juguetona |
| Canelo | 8 años | Mediano | Macho | Movilidad reducida leve y ritmo tranquilo |
| Oliva | 2 años | Mediano | Hembra | Mestiza de border collie muy activa |
| Mora | 3 años | Chico | Hembra | Tímida; adaptación gradual |
| Dante | 4 años | Grande | Macho | Mestizo de pastor, atento |
| Lola | 5 años | Mediano | Hembra | Adopción conjunta con Simón |
| Simón | 5 años | Mediano | Macho | Adopción conjunta con Lola |
| Pipa | 3 meses | Chico | Hembra | Cachorra con cuidados pendientes |
| Nube | 10 años | Chico | Macho | Senior tranquilo |
| Sol | 18 meses | Grande | Hembra | Mestiza de labrador, enérgica |

Las diez imágenes fueron generadas con la herramienta integrada `image_gen`; no corresponden a fotografías documentales de rescates. Están en [tools/cloud/content/photos](../tools/cloud/content/photos). Las especificaciones de generación están en [image-sources.json](../tools/cloud/content/image-sources.json). Se ajustaron a JPEG de hasta 100 KiB para utilizar el almacenamiento existente en Firestore, sin Cloud Storage ni servicios de pago.

## Pantallas y textos revisados

La revisión de código y recursos abarcó todas las pantallas y sus variantes, incluidos estados vacíos, errores y mensajes de formularios. No hay correos o notificaciones simuladas con los términos retirados en las colecciones consultadas; no se enviaron mensajes ni se alteraron cuentas de Authentication.

| Pantallas revisadas | Textos modificados o resultado |
| --- | --- |
| Bienvenida, inicio de sesión de adoptante/refugio, registro | Retirado el aviso de entorno local; correos prefijados de variantes visuales cambiados a dominios reservados `.invalid`. |
| Catálogo, búsqueda, filtros, sin resultados, sin conexión y errores | Retirado el aviso del entorno. El nombre del refugio y los totales proceden de los datos actuales. Mensajes funcionales conservados; contador en singular cuando hay un resultado. |
| Ficha pública y ficha privada del perrito | Retirados los avisos de origen del contenido. El folio técnico del perrito no se imprime en la ficha privada; sus identificadores se conservan en la base de datos. |
| Información del refugio y Google Maps | Retiradas las etiquetas del origen del refugio y la ubicación; nombre, zona y horario actualizados. |
| Requisitos de adopción | Retirado el banner de origen; redacción de condiciones actualizada. |
| Solicitud: contacto, hogar, otras mascotas, motivos, revisión y confirmación | Retirado el aviso de origen de la solicitud; datos de variantes visuales sin correos de muestra. Formularios y validaciones conservados. |
| Mis solicitudes y detalle | Nombre del refugio y del perrito actualizados desde los datos; nombre y motivos técnicos de una solicitud existente reemplazados. Los demás datos se conservaron. |
| Perfil de visitante y adoptante | Revisados; sin nuevos cambios. Acceso para refugios del visitante conservado. |
| Mis perritos, registro, edición, vista previa y publicación | Retirados avisos de origen del registro y la publicación; nombre del refugio unificado en variantes visuales. |
| Propuesta de Gemini, aceptación y error | Revisados; sin términos de origen ni cambios en su funcionamiento. |
| Solicitudes recibidas, evaluación y decisión | Nombre del refugio unificado; correos de variantes visuales con dominio reservado. Decisiones e historial conservados. |
| Entrega, carta compromiso, confirmación y bloqueo de adopción | Revisados; nombres actuales a partir de las fichas. Fechas, cartas y estados conservados. |
| Seguimientos, programación, registro, confirmación, detalle e historial | Observaciones técnicas de un seguimiento reemplazadas por una nota de adaptación. Fechas y relaciones conservadas. |
| Perfil del refugio y editar información | Retirada la etiqueta de origen del refugio. Campos y acceso de edición conservados. |
| Acceso restringido y errores de permisos | Revisados; mensajes funcionales conservados. |
| Galería interna de pantallas | Título «Vistas de la aplicación» y explicación «Recorre las pantallas y consulta sus diferentes estados». Sigue oculta en la compilación normal. |
| Nombre de la app y recursos Android | «Huellitas Chalco» conservado; sin avisos en título o metadatos visibles. No hay pestañas web en la app Android. |

Los identificadores de documentos, claves internas, banderas y configuración de emuladores no se renombraron: no son textos de interfaz y cambiarlos rompería referencias existentes. El respaldo anterior a la actualización está fuera del repositorio, en la carpeta privada de trabajo.

## Comprobaciones

- Lectura pública de Firebase sin iniciar sesión: 17 perritos en el catálogo y diez imágenes nuevas íntegramente descargables.
- Totales: 6 chicos, 6 medianos, 5 grandes; 5 menores de 12 meses, 12 desde 12 meses; 9 hembras, 8 machos. Los seniors participan en Adulto porque son los filtros ya existentes.
- Se agregaron 10 perritos, 10 controles y 10 imágenes; cada documento se creó solo si no existía.
- Comparación de los 15 documentos editados contra el respaldo: solo se modificaron los campos editoriales previstos y las fechas de actualización correspondientes.
- Actualización atómica con precondiciones de versión para no sobrescribir cambios concurrentes.

Herramientas: `node tools/cloud/verify-content.mjs` verifica el contenido público de esta entrega. `update-content.mjs` realiza una vista previa por defecto y requiere `--write` y un respaldo privado para publicar; se niega a reemplazar altas existentes. No debe ejecutarse nuevamente para crear duplicados.

Resultado de la entrega: 126 pruebas unitarias sin fallos; compilación y Lint correctos. En la tableta se verificaron el perfil del refugio, el catálogo de 17 perritos, los filtros Chico (6) y Cachorro (5), la búsqueda de Chispa, su ficha con imagen cargada, requisitos e información pública con el panel de Maps. La revisión de las demás variantes y pantallas fue estática sobre código y recursos; no se enviaron solicitudes ni se formalizaron nuevas adopciones durante esta revisión.

En la instalación final también se comprobaron solicitudes recibidas, seguimientos, búsqueda sin acento «Simon» → Simón, el contador «1 perrito busca hogar» y la ficha de adopción conjunta. La condición de adopción conjunta de Lola y Simón se comunica en sus descripciones y queda a cargo de la evaluación del refugio; no se añadieron reglas automáticas ni nuevas relaciones a la base de datos.
