package no.fint.orgmonitor

import no.fint.orgmonitor.organization.OrganizationDocument
import no.fint.orgmonitor.organization.OrganizationRepository
import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrganizationRepositoryTest(
    @param:Autowired private val organizationRepository: OrganizationRepository,
    @param:Autowired private val mongoTemplate: MongoTemplate,
) : BaseIntegrationTest() {
    @AfterEach
    fun cleanup() {
        organizationRepository.deleteAll()
    }

    @Test
    fun `getAllByOrgId returns only documents matching the given orgId`() {
        organizationRepository.save(OrganizationDocument(id = "1", orgId = "fintlabs.no"))
        organizationRepository.save(OrganizationDocument(id = "2", orgId = "fintlabs.no"))
        organizationRepository.save(OrganizationDocument(id = "3", orgId = "annen.no"))

        val result = organizationRepository.getAllByOrgId("fintlabs.no")

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllByOrgId returns empty list when no documents match`() {
        organizationRepository.save(OrganizationDocument(id = "1", orgId = "annen.no"))

        val result = organizationRepository.getAllByOrgId("fintlabs.no")

        assertEquals(0, result.size)
    }

    @Test
    fun `getOrganizationDocumentByIdAndOrgId returns document when both id and orgId match`() {
        organizationRepository.save(OrganizationDocument(id = "1", orgId = "fintlabs.no"))

        val result = organizationRepository.getOrganizationDocumentByIdAndOrgId("1", "fintlabs.no")

        assertNotNull(result)
        assertEquals("1", result.id)
    }

    @Test
    fun `getOrganizationDocumentByIdAndOrgId returns null when orgId does not match`() {
        organizationRepository.save(OrganizationDocument(id = "1", orgId = "other.no"))

        val result = organizationRepository.getOrganizationDocumentByIdAndOrgId("1", "fintlabs.no")

        assertNull(result)
    }

    @Test
    fun `getOrganizationDocumentByIdAndOrgId returns null when id does not exist`() {
        val result = organizationRepository.getOrganizationDocumentByIdAndOrgId("nonexistent", "fintlabs.no")

        assertNull(result)
    }

    @Test
    fun `save and read keeps organisasjonsnummer in nested organization data`() {
        organizationRepository.save(
            OrganizationDocument(
                id = "fintlabs.no:1001",
                orgId = "fintlabs.no",
                data =
                    Organisasjonselement().apply {
                        organisasjonsId = Identifikator().apply { identifikatorverdi = "1001" }
                        organisasjonsnummer = Identifikator().apply { identifikatorverdi = "999999999" }
                    },
            ),
        )

        val stored = organizationRepository.findById("fintlabs.no:1001").orElseThrow()
        assertEquals("999999999", stored.data?.organisasjonsnummer?.identifikatorverdi)

        val raw =
            mongoTemplate
                .getCollection("organization_document")
                .find(Document("_id", "fintlabs.no:1001"))
                .first()
        val rawOrganizationNumber =
            raw
                ?.get("data", Document::class.java)
                ?.get("organisasjonsnummer", Document::class.java)
                ?.getString("identifikatorverdi")

        assertEquals("999999999", rawOrganizationNumber)
    }
}
