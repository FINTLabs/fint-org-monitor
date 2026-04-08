package no.fint.organization

import no.fint.Config
import no.fint.mailing.MailingService
import no.fint.utils.ResourceConverter
import no.fint.utils.RestUtil
import no.fint.utils.TemplateService
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
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

    private data class UpdateChanges(
        val addedDocuments: MutableList<OrganizationDocument> = mutableListOf(),
        val documentsToSave: MutableList<OrganizationDocument> = mutableListOf(),
        val updatedPairs: MutableList<Pair<OrganizationDocument, OrganizationDocument>> = mutableListOf(),
        val parentIds: MutableSet<String> = linkedSetOf(),
    ) {
        fun hasChanges(): Boolean = addedDocuments.isNotEmpty() || updatedPairs.isNotEmpty()
    }

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
        val documents = organizationRepository.getAllByOrgId(config.orgid)
        logger.info("Repository contains ${documents.size} documents for ${config.orgid}.")
        val organizationMap = documents.associateBy { it.data?.organisasjonsId?.identifikatorverdi }

        val updates = fetchUpdates() ?: return
        logger.info("Found ${updates.size} updates")
        logger.trace(
            "Updates content: {}",
            updates.content.joinToString(", ") { "ID=${it.organisasjonsId.identifikatorverdi}, Name=${it.navn}" },
        )
        val changes = collectChanges(updates.content, organizationMap)

        logger.info("Saving {} updates", changes.documentsToSave.size)
        organizationRepository.saveAll(changes.documentsToSave)
        logger.info("Added: {} items", changes.addedDocuments.size)
        logger.info("Updated: {} items", changes.updatedPairs.size)

        sendNotification(changes)
    }

    private fun fetchUpdates(): OrganisasjonselementResources? {
        val updates =
            restUtil.getUpdates(
                object : ParameterizedTypeReference<OrganisasjonselementResources?>() {},
            )

        if (updates == null) {
            logger.error("Failed to fetch updates from endpoint")
        }

        return updates
    }

    private fun collectChanges(
        resources: List<OrganisasjonselementResource>,
        organizationMap: Map<String?, OrganizationDocument>,
    ): UpdateChanges {
        val changes = UpdateChanges()

        resources.forEach { resource ->
            val resourceId = resource.organisasjonsId.identifikatorverdi
            val newDocument = createDocument(resource)
            val existingDocument = organizationMap[resourceId]

            if (existingDocument == null) { // New document
                storeNewDocument(newDocument, changes, organizationMap)
            } else if (existingDocument != newDocument) { // Changed document
                storeModifiedDocument(newDocument, resourceId, existingDocument, changes, organizationMap)
            }
            // If none of the above, then we do nothing as there is no change.
        }

        return changes
    }

    private fun storeModifiedDocument(
        modifiedDocument: OrganizationDocument,
        resourceId: String,
        existingDocument: OrganizationDocument,
        changes: UpdateChanges,
        organizationMap: Map<String?, OrganizationDocument>,
    ) {
        modifiedDocument.id = resourceId
        changes.documentsToSave.add(modifiedDocument)

        // A little ugly to use .copy(), but necessary to avoid existingDocument being modified after it is added to updatedPairs
        // The pointer to underordnet is copied.
        changes.updatedPairs.add(
            Pair(
                existingDocument.copy(underordnet = existingDocument.underordnet?.toList()),
                modifiedDocument,
            ),
        )

        registerParentId(modifiedDocument.overordnet, organizationMap, changes)
    }

    private fun storeNewDocument(
        newDocument: OrganizationDocument,
        changes: UpdateChanges,
        organizationMap: Map<String?, OrganizationDocument>,
    ) {
        changes.documentsToSave.add(newDocument)
        changes.addedDocuments.add(newDocument)

        registerParentId(newDocument.overordnet, organizationMap, changes)
    }

    private fun registerParentId(
        parentHref: String?,
        organizationMap: Map<String?, OrganizationDocument>,
        changes: UpdateChanges,
    ) {
        if (!StringUtils.hasText(parentHref)) {
            return
        }

        organizationMap[parentHref]?.id?.let(changes.parentIds::add)
    }

    private fun sendNotification(changes: UpdateChanges) {
        if (!changes.hasChanges()) {
            return
        }

        val parentInfo = createParentInfo(changes.parentIds.toList())
        mailingService.send(templateService.render(changes.addedDocuments, changes.updatedPairs, parentInfo))
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
