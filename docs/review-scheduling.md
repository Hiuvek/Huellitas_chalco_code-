# HU-15 — Programar revisiones

Desde una adopción registrada se elige la fecha (AAAA-MM-DD) y el tipo: visita a domicilio, llamada o videollamada. La fecha debe existir y no ser anterior a la entrega. Una revisión pasada conserva su fecha; la app no inventa una nueva ni un resultado realizado.

La función autenticada `programReview` consulta perfil, membresía, refugio y adopción dentro de la transacción. Crea un documento `revisiones` con los campos del modelo original: adopcionId, perritoId, refugioId, fechaProgramada, tipo, estado=PROGRAMADA, seguimientoId=null y creadoEn del servidor. El ID del intento se conserva al reintentar para evitar citas duplicadas.

La agenda consulta solo el refugio autorizado, separa Programados de Adopciones y muestra fechas y tipos reales. Abrir revisión conserva su ID y muestra la cita seleccionada. La captura y el guardado del resultado se completan con HU-16; ese criterio se mantiene pendiente hasta integrar su formulario.

La confirmación conserva fecha y tipo ante un fallo de conexión. Un rechazo definitivo permite corregirlos. Se confirma únicamente después de la respuesta del servidor. Volver sin programar no crea una cita ficticia.

## Preparación escolar

Arrancar los cuatro emuladores, con Functions en 5001. Ejecutar en este orden: seed:local, seed:catalog, seed:requests y seed:adoptions. El último usa las operaciones reales de revisión, aprobación y entrega para preparar las dos adopciones ficticias A/B. No crea revisiones por adelantado. Requiere las dependencias de `functions` instaladas.

Si las entregas de demostración ya existen, seed:adoptions las conserva. seed:requests restablece también las entregas y revisiones de esos dos perritos ficticios; no se debe ejecutar si se quieren conservar los avances del escenario de demostración.

Entrar con la cuenta responsable A o B, abrir Seguimientos → Adopciones, seleccionar Programar revisión, capturar fecha y tipo y confirmar. La cita debe aparecer en Programados con su información y abrir la revisión correcta. Una cuenta ajena no puede consultar ni programar para otro refugio.

Se conserva Spark y no se despliega el backend al proyecto real. Ver [adoption-delivery.md](adoption-delivery.md) para la conexión local autenticada y los puertos.

## Verificación

94 pruebas unitarias totales y 11 del backend sin fallos. En Android pasaron 25 pruebas de SDK y regresión; la prueba restante de interfaz pasó al esperar la carga real de la revisión seleccionada. Compilación local y Android Lint correctos. Las reglas no cambian en HU-15; se conserva la suite anterior de 85 casos.
