package com.example.huellitaschalco.nucleo.fechas

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun civilDateMillis(value: String): Long? =
    if (value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")))
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("America/Mexico_City")
                }
                .parse(value)
                ?.time
                ?.takeIf { it > 0 }
        }
            .getOrNull()
    else null

fun civilDateLabel(milliseconds: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-MX"))
        .apply { timeZone = TimeZone.getTimeZone("America/Mexico_City") }
        .format(java.util.Date(milliseconds))
