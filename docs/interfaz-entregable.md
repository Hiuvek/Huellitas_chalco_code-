# Interfaz para presentación — versión 1.2

## Jerarquía y lectura

- Títulos de pantalla y secciones distinguen el propósito, la información y la siguiente acción.
- Texto de lectura y botones de 16 sp, ayudas de 13 sp y mayor contraste en los textos secundarios. Se respetan las preferencias de tamaño de letra del dispositivo.
- Encabezados de sección marcados para navegación con lector de pantalla; botones con altura mínima de 50 dp.
- Botón relleno para la acción principal, contorno para acciones secundarias y enlaces para opciones de menor prioridad.

## Guía de acciones

| Pantalla | Cómo se orienta al usuario |
| --- | --- |
| Bienvenida | Explorar perritos es la acción principal; se explica que no se necesita cuenta para ver el catálogo. Inicio de sesión y registro son secundarios. |
| Catálogo | Indicación de elegir una tarjeta; cada una muestra «Ver ficha de…» con una flecha. |
| Ficha del perrito | Botón fijo «Ver requisitos para adoptar». Acceso secundario «Ver refugio y ubicación», con Google Maps en la información del refugio. |
| Requisitos | Instrucciones previas y botón fijo «Iniciar solicitud». Si está en proceso, se explica junto al botón por qué no admite solicitudes nuevas. |
| Solicitud | Paso actual, nombre del paso siguiente y explicación de que todavía no se ha enviado. Último botón «Enviar solicitud». Salir dice «Salir y conservar borrador». |
| Mis solicitudes | Cada tarjeta indica «Ver estado y respuestas». |
| Solicitudes recibidas | Instrucción de seleccionar una solicitud y acción visible «Revisar solicitud». |
| Mis perritos | Registrar uno nuevo es la acción principal; las tarjetas indican «Ver datos y salud». |
| Seguimientos | «Registrar seguimiento» sustituye la acción genérica «Abrir revisión». |
| Perfil del refugio | Accesos a funciones reales, zona y horario del refugio, requisitos publicados y Maps. |
| Mi cuenta | Acceso a solicitudes y catálogo; explica los borradores y el uso sin conexión sin mostrar cifras o fechas inventadas. |

Los registros de ejemplo se identifican como demostración escolar. Ya no se afirma que solo existen en emuladores cuando pertenecen al catálogo real.

## Muestrario

La compilación habitual oculta el icono del ojo, el enlace del perfil y la entrada al muestrario. La opción de desarrollo `-PshowPreviewGallery=true` permite habilitarlo explícitamente en debug; release mantiene esa opción desactivada. Las pruebas de pantallas pueden seguir inicializando destinos de forma aislada.

## Alcance

Esta actualización cambia presentación, orientación y accesos del perfil. El APK sigue siendo una compilación debug firmada para presentación privada, no una publicación en Google Play.

## Verificación del 22 de noviembre de 2026

- Compilación normal 1.2 (código 3), 114 pruebas unitarias y Android Lint correctos.
- 39 comprobaciones de interfaz correctas después de ajustar las expectativas de las pruebas a las nuevas etiquetas y botones fijos. Incluyen acceso, catálogo, solicitud, perfiles, permisos, evaluación, Maps, funcionamiento local y seguimientos.
- Revisión visual en la tableta con ancho temporal de 411 dp: bienvenida, catálogo, ficha, requisitos y perfiles. Bienvenida comprobada con letra al 130 %. Tamaño, densidad y escala de letra originales restaurados al terminar.
- APK normal actualizado en la tableta y copia en Descargas/Huellitas-Chalco-Spark. La captura final confirma que se retiró el aviso antiguo de muestrario en la administración.

Los registros de ejecución se conservaron en el directorio de trabajo local de esta revisión: `ui-hierarchy-delivery-build.log`, `ui-hierarchy-flows.log`, `ui-hierarchy-regressions.log`, `ui-hierarchy-delivery.log`, `ui-hierarchy-followups.log` y `ui-phone-review.log`. Las dos comprobaciones que inicialmente esperaban etiquetas o conteos antiguos se corrigieron y volvieron a pasar. Los 39 casos se cuentan una sola vez.

Esta revisión no repite las 85 pruebas de reglas de la adaptación a Spark ni equivale a una prueba en dos dispositivos físicos.

## Ajustes de la versión 1.2.1

La fecha real de entrega se inicializa al día actual en la zona de Ciudad de México, con formato AAAA-MM-DD. Se puede editar antes de confirmar para indicar una entrega anterior. Se mantienen el rechazo de fechas futuras y la confirmación de entrega y carta. Las entregas ya registradas conservan su fecha original.

Cuando el cierre de otras solicitudes termina, aparece «Cierre completado. No quedan otras solicitudes pendientes para este perrito.» y se oculta su botón. Antes seguía visible aunque el cierre ya se había confirmado, lo que hacía parecer que no respondía al pulsarlo de nuevo. No se modificaron las transacciones ni los permisos. Al volver a abrir una entrega guardada se puede verificar/completar el cierre de nuevo; no crea otra adopción.

Validación 1.2.1: compilación normal, 114 pruebas unitarias y Lint correctos. La prueba de interfaz de entrega comprobó hoy por defecto, cambio a ayer y persistencia de la fecha modificada en emuladores. Después se ajustó la visibilidad del cierre y se verificó manualmente en la app normal contra Firebase real con la entrega ya guardada de Kira: tras verificar el cierre aparece el mensaje de confirmación y desaparece el botón, sin registrar otra adopción.

Evidencia local: `delivery-date-ui.log`, `delivery-date-final-build.log` y `delivery-closure-fixed.png`. APK 1.2.1 (código 4), firma original, instalado en la tableta y copiado a Descargas/Huellitas-Chalco-Spark.

## Paleta de la versión 1.2.2

Por elección del usuario, los colores principal, secundario y terciario usan verde bosque `#4D6550`, con contenedores salvia `#E3E9DF`. Los componentes Material de selección, filtros, pestañas y formularios heredan esta paleta global. Los controles personalizados conservan el mismo verde.

Los estados rutinarios En proceso, En revisión y Esta semana dejan de usar terracota. No y No aprobada usan un tono neutro, con texto e icono para distinguirlos sin depender del color. El mapa ilustrativo normal y Cerrar sesión también dejan de usar rojo. Los errores, instrucciones críticas y acciones destructivas conservan terracota `#B24B32` con fondo suave cuando corresponde.

El cambio solo afecta la presentación: no modifica estados de solicitudes, permisos ni operaciones en Firebase.

Verificación 1.2.2: compilación normal y Android Lint correctos. Se revisaron los colores heredados por los controles Material y los usos directos de terracota; la revisión visual en la tableta confirmó el foco verde del formulario de seguimiento. Se conserva la evidencia local en `forest-theme-build.log` y `forest-selection-tablet.png`. Esta actualización visual no repitió las pruebas funcionales anteriores. APK firmado con la clave original, instalado sin borrar datos y actualizado en Descargas.

## Organización del perfil — versión 1.2.3

El perfil del refugio se concentra en cuatro bloques: información pública (zona, horario y un solo enlace a Google Maps), proceso de adopción desplegable, acceso al catálogo para adoptantes y cuenta responsable. Los requisitos y pasos se pueden abrir y cerrar sin navegar a otra pantalla. El estado de esa sección se conserva al recrear la pantalla y se separa por refugio.

Se retiraron los botones de perritos, solicitudes y seguimientos que duplicaban las pestañas inferiores, el mapa como segunda tarjeta de ubicación y la identidad del refugio repetida dentro del contenido. El encabezado conserva el refugio actual y su selector cuando hay varios. La cuenta y el cierre de sesión permanecen al final. Los datos provienen de los repositorios existentes y la información guardada o los errores de actualización se indican junto a sus datos.

En Solicitudes recibidas se quitó el subtítulo con el nombre del refugio: ahora solo lo muestra el encabezado compartido, conservando la selección entre refugios.

Verificación 1.2.3: compilación y Lint correctos. Revisión visual de la sección de proceso contraída y expandida en la tableta. Se comprobó mediante el árbol de interfaz que Solicitudes recibidas muestra una sola coincidencia del nombre del refugio. Capturas locales: `profile-cleanup-expanded.png`, `profile-cleanup-collapsed.png` y `requests-single-refuge.png`. No se ejecutaron nuevamente las pruebas funcionales completas; los intentos de comprobar automáticamente el estado contraído no se contabilizan como pruebas aprobadas, debido a que la pantalla cambió durante la revisión. El cambio no altera los repositorios ni permisos.

APK 1.2.3 (código 6), compilación normal firmada con la clave original, instalado y copiado a Descargas. Registro final: `profile-cleanup-final-build.log`.
