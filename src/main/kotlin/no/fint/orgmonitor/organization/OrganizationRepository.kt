package no.fint.orgmonitor.organization

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : MongoRepository<OrganizationDocument, String> {
    fun getAllByOrgId(orgId: String): List<OrganizationDocument>

    fun getOrganizationDocumentByIdAndOrgId(
        id: String,
        orgId: String,
    ): OrganizationDocument?
}
