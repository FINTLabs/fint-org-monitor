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

/**
 * Service responsible for monitoring and updating organization elements.
 *
 * This service fetches organization data from a remote endpoint, compares it with the local database,
 * and identifies added or modified elements. If changes are detected, it updates the database
 * and triggers an email notification with a summary of the changes.
 *
 */
@Service
class OrganizationService(
    private val config: Config,
    private val organizationRepository: OrganizationRepository,
    private val restUtil: RestUtil,
    private val mailingService: MailingService,
    private val templateService: TemplateService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Orchestrates the update process for organization elements.
     *
     * This method performs the following steps:
     * 1. Retrieves all existing organization documents from the repository.
     * 2. Fetches the latest updates from the configured REST endpoint.
     * 3. Compares the fetched resources with existing documents to detect additions and modifications.
     * 4. Persists the changes (new and updated documents) to the database.
     * 5. If changes were found, compiles a report (including parent info) and sends it via email.
     */
    fun update() {
        val addedOrganizationDocuments = mutableListOf<OrganizationDocument>()
        val updatedOrganizationDocuments = mutableListOf<OrganizationDocument>()
        val updatedPairs = mutableListOf<Pair<OrganizationDocument, OrganizationDocument>>() // Old and New value for comparison
        val parentIds = mutableListOf<String>()

        // Get all documents by orgid
        val documents = organizationRepository.getAllByOrgId(config.orgid)
        logger.info("Repository contains ${documents.size} documents for ${config.orgid}.")

        // Create map to lookup documents by the organizations id
        val organizationMap = documents.associateBy { it.data?.organisasjonsId?.identifikatorverdi }

        // Get updates from endpoint
        val updates =
            restUtil.getUpdates(
                object : ParameterizedTypeReference<OrganisasjonselementResources?>() {},
            )

        if (updates == null) {
            logger.error("Failed to fetch updates from endpoint")
            return
        }

        logger.info("Found ${updates.size} updates")
        logger.trace(
            "Updates content: {}",
            updates.content.joinToString(", ") { "ID=${it.organisasjonsId.identifikatorverdi}, Name=${it.navn}" },
        )
        // Go through each resource to check if it's new or modified.
        // If it is modified, construct and save the necessary objects to be able to create a report showing the differences.
        updates.content.forEach { resource ->
            val resourceId = resource.organisasjonsId.identifikatorverdi
            val newDocument = createDocument(resource)

            organizationMap[resourceId]?.let { existingDocument ->
                // if the document exists in the database, we have to check if it actually has been modified.
                if (existingDocument != newDocument) {
                    // ... and store it if it has been modified
                    storeModifiedDocument(
                        newDocument,
                        resourceId,
                        updatedOrganizationDocuments,
                        updatedPairs,
                        existingDocument,
                        organizationMap,
                        parentIds,
                    )
                }
            } ?: run {
                // If current == null
                storeNewDocument(
                    createDocument(resource),
                    updatedOrganizationDocuments,
                    addedOrganizationDocuments,
                    organizationMap,
                    parentIds,
                )
            }
        }

        logger.info("Saving {} updates", updatedOrganizationDocuments.size)
        organizationRepository.saveAll(updatedOrganizationDocuments)

        logger.info("Added: {} items", addedOrganizationDocuments.size)

        logger.info("Updated: {} items", updatedPairs.size)

        if (addedOrganizationDocuments.isNotEmpty() || updatedPairs.isNotEmpty()) {
            val parentInfo = if (parentIds.isEmpty()) mutableListOf() else createParentInfo(parentIds)
            mailingService.send(templateService.render(addedOrganizationDocuments, updatedPairs, parentInfo))
        }
    }

    private fun storeModifiedDocument(
        modifiedDocument: OrganizationDocument,
        resourceId: String,
        updatedOrganizationDocuments: MutableList<OrganizationDocument>,
        updatedPairs: MutableList<Pair<OrganizationDocument, OrganizationDocument>>,
        existingDocument: OrganizationDocument,
        organizationMap: Map<String?, OrganizationDocument>,
        parentIds: MutableList<String>,
    ) {
        modifiedDocument.id = resourceId
        updatedOrganizationDocuments.add(modifiedDocument)

        // A little ugly, but necessary to avoid existingDocument being modified after it is added to updatedPairs
        // The pointer to underordnet is also copied as the its only a pointer.
        updatedPairs.add(
            Pair(
                existingDocument.copy(underordnet = existingDocument.underordnet?.toList()),
                modifiedDocument,
            ),
        )

        // Add parentId to parentIds list, if parent exists
        if (StringUtils.hasText(modifiedDocument.overordnet)) {
            organizationMap[modifiedDocument.overordnet]?.let { parentDocument ->
                if (!parentIds.contains(parentDocument.id)) {
                    parentIds.add(parentDocument.id)
                }
            }
        }
    }

    private fun storeNewDocument(
        newDocument: OrganizationDocument,
        updatedOrganizationDocuments: MutableList<OrganizationDocument>,
        addedOrganizationDocuments: MutableList<OrganizationDocument>,
        organizationMap: Map<String?, OrganizationDocument>,
        parentIds: MutableList<String>,
    ) {
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
