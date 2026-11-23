# Editar información del refugio

Versión 1.4.0. Desde **Perfil → Editar información**. Solo se ofrece con datos actuales del servidor y dentro de un refugio autorizado.

## Campos

- Nombre: 1 a 100 caracteres.
- Zona pública y horario: 1 a 250 caracteres cada uno.
- Requisitos y pasos: de 1 a 10 entradas por lista, una por línea y hasta 300 caracteres por entrada. Se conservan en su orden.
- Coordenadas opcionales: ambas numéricas y dentro de los rangos de latitud y longitud, o ambas vacías. Sin coordenadas, Maps utiliza la zona pública de Chalco.

No permite cambiar el ID, municipio, entidad, activación, marca de demostración o membresías. Crear refugios y asignar responsables sigue siendo una operación administrativa fuera de la app.

## Guardado

Una transacción compara los datos actuales con la versión que se abrió. Si otro responsable los cambió, no sobrescribe esa edición: conserva el formulario y ofrece recargar con aviso de que se perderán los cambios locales. Repetir una operación ya guardada no vuelve a escribir ni cambia su fecha.

Se confirma después de la respuesta del servidor. Un error conserva las modificaciones para reintentar; no se guarda sin conexión. Guardar y mensajes de error quedan en la parte inferior fija. Cancelar o regresar con cambios muestra una confirmación para descartarlos.

Firestore registra `actualizadoEn` con hora del servidor y `actualizadoPorUid` con la cuenta responsable. Las reglas verifican perfil, membresía y refugio activos, límites de campos y autoría. Los demás usuarios pueden consultar la información pública actualizada, pero no editarla.

## Pruebas

`ShelterEditingTest` comprueba validaciones, precisión de coordenadas, normalización, doble toque, errores, reintento y conflicto con recarga explícita. Las reglas se cubren en `shelter-edit.rules.test.mjs`.

`CloudShelterEditTest`, activado expresamente con `-e cloudShelterEdit true`, cambia temporalmente el horario del refugio de demostración, comprueba cancelación, lectura pública independiente, auditoría, conflicto e idempotencia y restaura los valores originales. La restauración usa la misma comprobación de conflictos para evitar sobrescribir cambios ajenos.

Resultado de la versión 1.4.0: 126 pruebas unitarias, 103 comprobaciones de reglas y 19 pruebas de interfaz aprobadas; compilación y Lint correctos. Las reglas se publicaron en Firebase real. La prueba de edición del refugio se ejecutó contra la nube y restauró el horario original. El nuevo flujo de Gemini al editar se comprobó con un generador controlado, sin consumir la API ni guardar automáticamente la ficha.
