package no.fint.organization

import no.fint.Config
import no.fint.mailing.MailingService
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
import no.fint.utils.ResourceConverter
import no.fint.utils.RestUtil
import no.fint.utils.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service
class OrganizationService(
    private val config: Config,
    private val organizationRepository: OrganizationRepository,
    private val restUtil: RestUtil,
    private val mailingService: MailingService,
    private val templateService: TemplateService,
) {
    private val logger = LoggerFactory.getLogger(OrganizationService::class.java)

    fun update() {
        val addedOrganizationDocuments = mutableListOf<OrganizationDocument>()
        val updatedOrganizationDocuments = mutableListOf<OrganizationDocument>()
        val updatedPairs = mutableListOf<Pair<OrganizationDocument, OrganizationDocument>>() // Old and New value for comparison
        val parentIds = mutableListOf<String>()

        // Get all documents by orgid
        val documents = organizationRepository.getAllByOrgId(config.orgid) // TODO: update to get only the document i want from database, instead of every?
        logger.info("Repository contains ${documents.size} documents.")

        // Create map to lookup documents by the organizations id
        val organizationMap = documents.associateBy { it.data?.organisasjonsId?.identifikatorverdi }

        // Get updates from endpoint
        val updates =
            restUtil.getUpdates(
                object : ParameterizedTypeReference<OrganisasjonselementResources?>() {},
                config.endpoint,
            )

        updates?.let { it ->
            logger.info("Found ${it.size} updates.")
            // Go through each resource to check if it's new or modified.
            // If it is modified, construct and save the necessary objects to be able to create a report showing the differences.
            it.content.forEach { resource ->
                val resourceId = resource.organisasjonsId.identifikatorverdi

                organizationMap[resourceId]?.let { existingDocument ->
                    // if current != null, the document exists in the database. So we have to check if it actually has been modified.
                    createDocument(resource).let { modifiedDocument ->
                        if (existingDocument != modifiedDocument) {
                            // If modified, store modifications
                            modifiedDocument.id = resourceId
                            updatedOrganizationDocuments.add(modifiedDocument)
                            updatedPairs.add(Pair(existingDocument, modifiedDocument))

                            // Add parentId to parentIds list, if parent exists
                            if (StringUtils.hasText(modifiedDocument.overordnet)) {
                                organizationMap[modifiedDocument.overordnet]?.let { parentDocument ->
                                    if (!parentIds.contains(parentDocument.id)) {
                                        parentIds.add(parentDocument.id)
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    // If current == null
                    createDocument(resource).let { newDocument ->
                        updatedOrganizationDocuments.add(newDocument)
                        addedOrganizationDocuments.add(newDocument)

                        // Add parent id to parentIds list, if parent exists
                        if (StringUtils.hasText(newDocument.overordnet)) {
                            organizationMap[newDocument.overordnet]?.let { parentDocument ->
                                if (!parentIds.contains(parentDocument.id)) {
                                    parentIds.add(parentDocument.id)
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            logger.error("Failed to fetch updates from endpoint: ${config.endpoint}")
            return
        }

        logger.info("Saving {} updates ...", updatedOrganizationDocuments.size)
        organizationRepository.saveAll(updatedOrganizationDocuments)

        logger.info("Added: {} items", addedOrganizationDocuments.size)

        logger.info("Updated: {} items", updatedPairs.size)

        if (addedOrganizationDocuments.isNotEmpty() || updatedPairs.isNotEmpty()) {
            val parentInfo = if (parentIds.isEmpty()) mutableListOf() else createParentInfo(parentIds)
            mailingService.send(templateService.render(addedOrganizationDocuments, updatedPairs, parentInfo))
        }
    }

    private fun createDocument(resource: OrganisasjonselementResource) =
        OrganizationDocument().apply {
            orgId = config.orgid
            data = ResourceConverter.toOrganisasjonselement(resource)
            overordnet = resource.overordnet.firstOrNull()?.href
            underordnet = resource.underordnet.map { it.href }
        }

    private fun createParentInfo(parentIds: List<String>): List<SimpleOrganizationInfo> =
        parentIds.mapNotNull { id ->
            organizationRepository.getOrganizationDocumentByIdAndOrgId(id, config.orgid)?.let { document ->
                document.data?.navn?.let { navn ->
                    SimpleOrganizationInfo(id, navn)
                }
            }
        }
}
