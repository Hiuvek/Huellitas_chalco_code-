package com.example.huellitaschalco.feature.refuge

import com.example.huellitaschalco.feature.refuge.domain.*
import java.net.URLDecoder
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class PublicMapTest {
    private val shelter =
        PublicShelter(
            "r",
            "Refugio",
            "Chalco",
            "Estado de México",
            "Centro de Chalco",
            "10 a 16 h",
            true,
        )

    @Test
    fun mapsUsesOnlyPublicZoneAndEncodedFixedGoogleEndpoint() {
        val url = shelter.copy(zone = "Zona A & B #1").googleMapsUrl()!!
        assertTrue(url.startsWith("https://www.google.com/maps/search/?api=1&query="))
        assertEquals(
            "Zona A & B #1, Chalco, Estado de México, México",
            URLDecoder.decode(url.substringAfter("&query="), "UTF-8"),
        )
        assertFalse(url.contains("& B"))
        assertFalse(url.contains("#1"))
    }

    @Test
    fun suppliedPublicCoordinatesAreRoundedAndLocaleIndependent() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val query =
                shelter
                    .copy(location = PublicCoordinates.checked(19.26415, -98.89718))
                    .googleMapsUrl()!!
                    .substringAfter("&query=")
            assertEquals("19.264,-98.897", URLDecoder.decode(query, "UTF-8"))
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun invalidCoordinatesNeverCreateAnInventedPin() {
        for (pair in
            listOf(
                91.0 to 1.0,
                1.0 to -181.0,
                Double.NaN to 1.0,
                1.0 to Double.POSITIVE_INFINITY,
            )) assertNull(PublicCoordinates.checked(pair.first, pair.second))
        assertNull(PublicCoordinates.fromStored(mapOf("latitud" to "19.2", "longitud" to -98.8)))
        assertNotNull(PublicCoordinates.fromStored(mapOf("latitud" to 19.2, "longitud" to -98.8)))
    }

    @Test
    fun missingZoneOrLocationOutsideChalcoDoesNotOfferMap() {
        assertNull(shelter.copy(zone = "").googleMapsUrl())
        assertNull(shelter.copy(zone = "Zona pública no registrada").googleMapsUrl())
        assertNull(shelter.copy(municipality = "Otro").googleMapsUrl())
    }
}
