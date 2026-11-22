package com.example.huellitaschalco.feature.refuge.domain

import java.net.URLEncoder
import java.util.Locale

@ConsistentCopyVisibility
data class PublicCoordinates private constructor(val latitude: Double, val longitude: Double) {
    companion object {
        fun checked(latitude: Double?, longitude: Double?): PublicCoordinates? =
            if (
                latitude != null &&
                    longitude != null &&
                    latitude.isFinite() &&
                    longitude.isFinite() &&
                    latitude in -90.0..90.0 &&
                    longitude in -180.0..180.0
            )
                PublicCoordinates(latitude, longitude)
            else null

        fun fromStored(raw: Any?): PublicCoordinates? {
            val map = raw as? Map<*, *> ?: return null
            return checked(
                (map["latitud"] as? Number)?.toDouble(),
                (map["longitud"] as? Number)?.toDouble(),
            )
        }
    }
}

fun PublicShelter.googleMapsUrl(): String? {
    if (municipality != "Chalco" || state != "Estado de México") return null
    val query =
        location?.let { String.format(Locale.ROOT, "%.3f,%.3f", it.latitude, it.longitude) }
            ?: zone
                .trim()
                .takeIf {
                    it.isNotBlank() && it != "Zona pública no registrada" && it.length <= 250
                }
                ?.let { "$it, $municipality, $state, México" }
            ?: return null
    return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, "UTF-8")
}

fun interface PublicMapLauncher {
    fun open(url: String)
}
