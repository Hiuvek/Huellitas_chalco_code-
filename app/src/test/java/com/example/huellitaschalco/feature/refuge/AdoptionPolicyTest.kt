package com.example.huellitaschalco.feature.refuge

import com.example.huellitaschalco.feature.refuge.domain.AdoptionPolicy
import org.junit.Assert.*
import org.junit.Test

class AdoptionPolicyTest {
    @Test
    fun loadsCompleteListsInTheirOriginalOrder() {
        val policy =
            AdoptionPolicy.fromStored(
                listOf(" Mayor de edad ", "Espacio seguro"),
                listOf(" Solicitud ", "Entrevista"),
            )!!
        assertEquals(listOf("Mayor de edad", "Espacio seguro"), policy.requirements)
        assertEquals(listOf("Solicitud", "Entrevista"), policy.steps)
    }

    @Test
    fun missingOrEmptyListsDoNotSupplyFakeRequirements() {
        for (raw in listOf(null, emptyList<String>())) {
            assertNull(AdoptionPolicy.fromStored(raw, listOf("Solicitud")))
            assertNull(AdoptionPolicy.fromStored(listOf("Mayor de edad"), raw))
        }
    }

    @Test
    fun wrongTypesAreRejectedRatherThanSilentlyDroppingConditions() {
        for (raw in
            listOf(
                "Condiciones",
                false,
                listOf("Identificación", false),
                listOf(mapOf("titulo" to "Condición")),
            )) {
            assertNull(AdoptionPolicy.fromStored(raw, listOf("Solicitud")))
            assertNull(AdoptionPolicy.fromStored(listOf("Identificación"), raw))
        }
    }

    @Test
    fun blankEntryInvalidatesTheWholePolicy() {
        assertNull(AdoptionPolicy.fromStored(listOf("Identificación", "  "), listOf("Solicitud")))
        assertNull(AdoptionPolicy.fromStored(listOf("Identificación"), listOf("Solicitud", "")))
    }
}
