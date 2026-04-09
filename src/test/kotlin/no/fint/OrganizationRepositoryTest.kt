package no.fint

import no.fint.organization.OrganizationDocument
import no.fint.organization.OrganizationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
}
