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
