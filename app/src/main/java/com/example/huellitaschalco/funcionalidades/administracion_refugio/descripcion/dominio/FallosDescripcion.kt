package com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.dominio

/** Mensajes controlados: nunca exponen respuestas del servidor, claves ni datos de la ficha. */
enum class TipoFalloDescripcion(val userMessage: String) {
    Connection(
        "No hay conexión con Gemini. Revisa tu internet y vuelve a intentar; tu descripción se conserva."
    ),
    AppVerification(
        "Esta instalación no está autorizada para usar Gemini. Solicita su autorización de App Check; puedes escribir manualmente."
    ),
    Quota(
        "Se alcanzó el límite de uso de Gemini. Intenta más tarde o escribe la descripción manualmente."
    ),
    Configuration(
        "Gemini no está disponible con la configuración actual. Solicita revisar la configuración; tu descripción se conserva."
    ),
    Timeout(
        "Gemini tardó demasiado. Puedes reintentar o escribir manualmente; tu descripción se conserva."
    ),
    Blocked(
        "Gemini no pudo redactar con estas notas. Revísalas o escribe la descripción manualmente."
    ),
    Unavailable(
        "Gemini no está disponible en este momento. Intenta más tarde o escribe manualmente; tu descripción se conserva."
    ),
}

class FalloServicioDescripcion(val kind: TipoFalloDescripcion) : Exception(kind.userMessage)
