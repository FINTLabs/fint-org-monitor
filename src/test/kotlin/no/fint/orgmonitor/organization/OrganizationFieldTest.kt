package no.fint.orgmonitor.organization

import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals

class OrganizationFieldTest {
    @Test
    fun `navn change highlights only navn`() {
        val before = document(navn = "Old name")
        val after = document(navn = "New name")

        assertEquals(setOf(OrganizationField.NAVN), OrganizationField.changedFields(before, after))
    }

    @Test
    fun `overordnet change highlights only overordnet`() {
        val before = document(overordnet = "parent-a")
        val after = document(overordnet = "parent-b")

        assertEquals(setOf(OrganizationField.OVERORDNET), OrganizationField.changedFields(before, after))
    }

    @Test
    fun `underordnet added or removed child highlights underordnet`() {
        val before = document(underordnet = listOf("child-a", "child-b"))
        val after = document(underordnet = listOf("child-a"))

        assertEquals(setOf(OrganizationField.UNDERORDNET), OrganizationField.changedFields(before, after))
    }

    @Test
    fun `underordnet reorder does not highlight underordnet`() {
        val before = document(underordnet = listOf("child-a", "child-b"))
        val after = document(underordnet = listOf("child-b", "child-a"))

        assertEquals(emptySet(), OrganizationField.changedFields(before, after))
    }

    @Test
    fun `nullable organization number and dates compare correctly`() {
        val before =
            document(
                organizationNumber = null,
                start = date("2019-04-01T00:00:00Z"),
                slutt = null,
            )
        val after =
            document(
                organizationNumber = "999999999",
                start = null,
                slutt = date("2026-07-03T00:00:00Z"),
            )

        assertEquals(
            setOf(
                OrganizationField.ORGANISASJONSNUMMER,
                OrganizationField.GYLDIGHETSPERIODE_START,
                OrganizationField.GYLDIGHETSPERIODE_SLUTT,
            ),
            OrganizationField.changedFields(before, after),
        )
    }

    private fun document(
        kortnavn: String? = "VGKALN",
        navn: String? = "VGKALN Kalnes videregående skole",
        kode: String? = "V40.44.17",
        organizationId: String? = "762",
        organizationNumber: String? = "974544520",
        overordnet: String? = "parent",
        underordnet: List<String>? = listOf("child-a", "child-b"),
        start: Date? = date("2019-04-01T00:00:00Z"),
        slutt: Date? = null,
    ): OrganizationDocument =
        OrganizationDocument(
            id = "fintlabs.no:${organizationId ?: "missing"}",
            orgId = "fintlabs.no",
            data =
                Organisasjonselement().apply {
                    this.kortnavn = kortnavn
                    this.navn = navn
                    if (kode != null) {
                        organisasjonsKode = Identifikator().apply { identifikatorverdi = kode }
                    }
                    if (organizationId != null) {
                        organisasjonsId = Identifikator().apply { identifikatorverdi = organizationId }
                    }
                    if (organizationNumber != null) {
                        organisasjonsnummer = Identifikator().apply { identifikatorverdi = organizationNumber }
                    }
                    gyldighetsperiode =
                        Periode().apply {
                            this.start = start
                            this.slutt = slutt
                        }
                },
            overordnet = overordnet,
            underordnet = underordnet,
        )

    private fun date(value: String): Date = Date.from(Instant.parse(value))
}
