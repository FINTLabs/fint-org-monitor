package no.fint.organization

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : JpaRepository<OrganizationDocument, String> {
    fun getAllByOrgId(orgId: String): List<OrganizationDocument>

    fun getOrganizationDocumentByIdAndOrgId(
        id: String,
        orgId: String,
    ): OrganizationDocument?
}
