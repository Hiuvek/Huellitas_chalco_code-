# Eliminar perritos de la lista del refugio

Versión 1.3.0. Desde **Perritos → abrir una ficha → Eliminar de la lista**. La confirmación muestra el nombre del perrito y permite **Conservar perrito** antes de efectuar cambios.

## Comportamiento

- Un perrito disponible se retira de Mis perritos y del catálogo público.
- Un perrito adoptado se retira de Mis perritos; conserva los datos y permisos vinculados a la adopción y sus seguimientos.
- Una adopción En proceso impide retirar la ficha hasta completar la entrega. La transacción y las reglas comprueban también el control de aprobación, por lo que no basta con modificar la interfaz.
- Solicitudes, historial, adopciones, revisiones, fotografías y seguimientos permanecen guardados. No se ejecuta un borrado físico de documentos.
- Las solicitudes pendientes conservan su estado. El refugio debe revisarlas y no aprobarlas desde Solicitudes; no puede aprobar una ficha disponible retirada del catálogo.
- No hay restauración desde la app. El diálogo explica este efecto antes de confirmar.

## Datos y permisos

`perritos/{id}` añade `eliminado: true`, `eliminadoEn` (fecha del servidor) y `eliminadoPorUid`. Actualiza `actualizadoEn`. Para Disponible, `publicado` pasa a false; para Adoptado se conserva su valor para mantener el acceso privado ya existente. Los registros antiguos sin `eliminado` siguen funcionando.

El listado del refugio excluye las fichas retiradas. Solo un responsable con perfil, membresía y refugio activos puede hacer esta operación en su refugio. No permite cambiar salud, nombre, estado, propietario ni referencias relacionadas durante la eliminación. Una ficha retirada no se puede editar, restaurar ni borrar físicamente desde el cliente.

La confirmación espera al servidor. Un fallo o tiempo agotado conserva el diálogo y permite reintentar sin repetir la eliminación ni cambiar su fecha original. La cancelación no escribe y el doble toque no inicia dos operaciones. Otro cliente conectado verá el cambio al recibir la actualización de Firebase; una copia de Room sin conexión puede seguir mostrando información antigua hasta actualizarse.

## Pruebas

Las reglas nuevas se incluyen en `npm --prefix tools/firebase run test:rules`. Se cubren el retiro de disponibles y adoptados, permisos, membresía revocada, integridad de campos, protección de aprobaciones, conservación del acceso privado y resolución de solicitudes conservadas.

`DogRemovalViewModelTest` cubre cancelación, doble toque, espera de confirmación, error, reintento y rechazo de la operación.

`CloudDogRemovalTest` solo se activa con `-e cloudDogRemoval true` en la compilación normal. Crea una ficha nueva con prefijo `spark-remove-` en el refugio escolar y comprueba cancelar y confirmar desde la interfaz, persistencia, idempotencia y rechazo de lectura pública. No retira fichas preexistentes. La ficha retirada permanece como evidencia privada.

## Resultado del 22 de noviembre de 2026

- 118 pruebas unitarias correctas, incluidas las cuatro del nuevo estado de eliminación.
- 94 pruebas de reglas correctas en el lote final, incluidas las nueve de retirada. El primer lote tuvo un error transitorio al limpiar Firestore Emulator y se repitió completo.
- Compilación y Lint correctos; reglas publicadas en el proyecto real, conservando Spark.
- Una prueba de interfaz integrada correcta contra Firebase real: cancelar conserva la publicación, confirmar vuelve a la lista sin la ficha, reintentar conserva la misma fecha de retirada y el cliente público independiente recibe rechazo de lectura. Se ajustó la inicialización de Firebase de la prueba antes del intento correcto.
- Ficha nueva de evidencia retirada: `spark-remove-37c3289a-89ec-4114-a26c-d388f119860b`. Ninguna ficha preexistente se retiró durante esta prueba.
- APK normal 1.3.0, código 7, firmado con la clave original e instalado sin borrar datos. Copia actualizada en Descargas/Huellitas-Chalco-Spark.

Registros locales fuera de Git: `dog-removal-build.log`, `dog-removal-rules-final.log`, `dog-removal-cloud-ui-final.log` y `dog-removal-deploy.log`.
