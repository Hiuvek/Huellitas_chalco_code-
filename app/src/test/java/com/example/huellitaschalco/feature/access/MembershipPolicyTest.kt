package com.example.huellitaschalco.feature.access

import com.example.huellitaschalco.feature.access.domain.*
import org.junit.Assert.*
import org.junit.Test

class MembershipPolicyTest {
    private val membership = Membership("uid__refugio-a", "uid", "refugio-a", true)
    private val shelter = Shelter("refugio-a", "Refugio A", "Chalco", "10 a 17 h")

    @Test
    fun activeMembershipMustMatchItsDocumentAndUser() {
        assertTrue(MembershipPolicy.validFor("uid", membership))
    }

    @Test
    fun anotherAccountNeverInheritsMembership() {
        assertFalse(MembershipPolicy.validFor("otro", membership))
    }

    @Test
    fun inactiveMembershipCannotGrantAccess() {
        assertFalse(MembershipPolicy.validFor("uid", membership.copy(active = false)))
    }

    @Test
    fun documentCannotPointToAnotherShelter() {
        assertFalse(MembershipPolicy.validFor("uid", membership.copy(shelterId = "refugio-b")))
    }

    @Test
    fun incompleteOrInvalidReferencesAreRejected() {
        for (id in listOf("", "a/b", "a.b", "a".repeat(101))) assertFalse(
            MembershipPolicy.validFor("uid", Membership("uid__$id", "uid", id, true))
        )
    }

    @Test
    fun permissionFromPreviousSessionCannotAuthorizeNewUser() {
        val access = ShelterAccess.Ready("uid", listOf(shelter))
        assertEquals(listOf(shelter), access.sheltersFor("uid"))
        assertTrue(access.sheltersFor("otro").isEmpty())
        assertTrue(access.sheltersFor(null).isEmpty())
    }

    @Test
    fun loadingErrorsAndLogoutAlwaysBlockAdministration() {
        for (access in
            listOf(
                ShelterAccess.SignedOut,
                ShelterAccess.Checking("uid"),
                ShelterAccess.Unavailable("uid"),
            )) assertTrue(access.sheltersFor("uid").isEmpty())
    }
}
