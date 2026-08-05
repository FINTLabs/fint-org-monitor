package no.fint.orgmonitor.organization

import no.fint.orgmonitor.Config
import no.fint.orgmonitor.mailing.MailingService
import no.fint.orgmonitor.sync.SyncState
import no.fint.orgmonitor.sync.SyncStateRepository
import no.fint.orgmonitor.utils.ResourceConverter
import no.fint.orgmonitor.utils.RestUtil
import no.fint.orgmonitor.utils.TemplateService
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    private val syncStateRepository: SyncStateRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class UpdateChanges(
        val addedDocuments: MutableList<OrganizationDocument> = mutableListOf(),
        val removedDocuments: MutableList<OrganizationDocument> = mutableListOf(),
        val documentsToSave: MutableList<OrganizationDocument> = mutableListOf(),
        val documentsToDelete: MutableList<OrganizationDocument> = mutableListOf(),
        val updatedPairs: MutableList<Pair<OrganizationDocument, OrganizationDocument>> = mutableListOf(),
        val parentIds: MutableSet<String> = linkedSetOf(),
    ) {
        fun hasChanges(): Boolean = addedDocuments.isNotEmpty() || removedDocuments.isNotEmpty() || updatedPairs.isNotEmpty()
    }

    /**
     * Orchestrates the update process for organization elements.
     *
     * This method performs the following steps:
     * 1. Retrieves all existing organization documents from the repository.
     * 2. Fetches the current source state from the configured REST endpoint.
     * 3. Compares the fetched resources with existing documents to detect additions, modifications and removals.
     * 4. Persists the changes to the database.
     * 5. If changes were found, compiles a report (including parent info and organization tree) and sends it via email.
     */
    fun update() {
        val documents = organizationRepository.getAllByOrgId(config.orgid)
        logger.info("Repository contains ${documents.size} documents for ${config.orgid}.")
        val organizationMap = documentsByOrganizationId(documents)

        val (currentState, lastUpdated) = fetchCurrentState() ?: return
        logger.info("Found ${currentState.size} organization elements in current source state")
        logger.trace(
            "Current state content: {}",
            currentState.content.joinToString(", ") { "ID=${it.organisasjonsId.identifikatorverdi}, Name=${it.navn}" },
        )
        val changes = collectChanges(currentState.content, documents, organizationMap)

        logger.info("Saving {} updates", changes.documentsToSave.size)
        organizationRepository.saveAll(changes.documentsToSave)
        logger.info("Deleting {} stale documents", changes.documentsToDelete.size)
        organizationRepository.deleteAll(changes.documentsToDelete)
        logger.info("Added: {} items", changes.addedDocuments.size)
        logger.info("Removed: {} items", changes.removedDocuments.size)
        logger.info("Updated: {} items", changes.updatedPairs.size)

        sendNotification(changes)

        // Persist the watermark only after the update has been processed successfully, so a
        // crash mid-processing leaves it untouched and the next run retries the same window.
        syncStateRepository.save(SyncState(config.orgid, lastUpdated))
    }

    private fun fetchCurrentState(): Pair<OrganisasjonselementResources, Long>? {
        val (currentState, lastUpdated) =
            restUtil.getCurrentState(
                object : ParameterizedTypeReference<OrganisasjonselementResources?>() {},
            )

        if (currentState == null) {
            logger.error("Failed to fetch current organization state from endpoint")
            return null
        }

        if (currentState.offset != 0 || currentState.content.size != currentState.totalItems) {
            logger.error(
                "Current organization state response is incomplete. offset={}, contentSize={}, totalItems={}. " +
                    "Skipping reconciliation to avoid deleting documents from a partial snapshot.",
                currentState.offset,
                currentState.content.size,
                currentState.totalItems,
            )
            return null
        }

        return currentState to lastUpdated
    }

    private fun collectChanges(
        resources: List<OrganisasjonselementResource>,
        existingDocuments: List<OrganizationDocument>,
        organizationMap: Map<String, OrganizationDocument>,
    ): UpdateChanges {
        val changes = UpdateChanges()
        val sourceOrganizationIds = linkedSetOf<String>()

        resources.forEach { resource ->
            val resourceId = resource.organisasjonsId.identifikatorverdi
            sourceOrganizationIds.add(resourceId)
            val newDocument = createDocument(resource)
            val existingDocument = organizationMap[resourceId]

            if (existingDocument == null) { // New document
                storeNewDocument(newDocument, changes, organizationMap)
            } else if (existingDocument != newDocument) { // Changed document
                storeModifiedDocument(newDocument, existingDocument, changes, organizationMap)
            }
            // If none of the above, then we do nothing as there is no change.
        }

        existingDocuments
            .filter { document -> document.organizationId() !in sourceOrganizationIds }
            .forEach { staleDocument -> storeRemovedDocument(staleDocument, changes, organizationMap) }

        return changes
    }

    private fun storeModifiedDocument(
        modifiedDocument: OrganizationDocument,
        existingDocument: OrganizationDocument,
        changes: UpdateChanges,
        organizationMap: Map<String, OrganizationDocument>,
    ) {
        // id is already set by createDocument from the resource's organisasjonsId
        changes.documentsToSave.add(modifiedDocument)

        // A little ugly to use .copy(), but necessary to avoid existingDocument being modified after it is added to updatedPairs
        changes.updatedPairs.add(
            Pair(
                existingDocument.copy(underordnet = existingDocument.underordnet?.toList()),
                modifiedDocument,
            ),
        )

        modifiedDocument.overordnet?.let { registerParentId(it, organizationMap, changes) }
    }

    // A removed document is one that exists locally but is absent from the complete current source snapshot.
    // Queue it for deletion and include it in the notification so stale MongoDB state cannot keep appearing in the tree.
    private fun storeRemovedDocument(
        removedDocument: OrganizationDocument,
        changes: UpdateChanges,
        organizationMap: Map<String, OrganizationDocument>,
    ) {
        changes.documentsToDelete.add(removedDocument)
        changes.removedDocuments.add(removedDocument)

        removedDocument.overordnet?.let { registerParentId(it, organizationMap, changes) }
    }

    private fun storeNewDocument(
        newDocument: OrganizationDocument,
        changes: UpdateChanges,
        organizationMap: Map<String, OrganizationDocument>,
    ) {
        changes.documentsToSave.add(newDocument)
        changes.addedDocuments.add(newDocument)

        newDocument.overordnet?.let { registerParentId(it, organizationMap, changes) }
    }

    private fun registerParentId(
        parentHref: String,
        organizationMap: Map<String, OrganizationDocument>,
        changes: UpdateChanges,
    ) {
        extractOrganizationId(parentHref)
            ?.let(organizationMap::get)
            ?.id
            ?.let(changes.parentIds::add)
    }

    private fun sendNotification(changes: UpdateChanges) {
        if (!changes.hasChanges()) {
            return
        }

        val parentInfo = createParentInfo(changes.parentIds.toList())
        // Reload after saveAll so the tree reflects the persisted post-update organization state.
        val organizationTree = OrganizationTreeBuilder.build(organizationRepository.getAllByOrgId(config.orgid))
        mailingService.send(
            templateService.render(
                changes.addedDocuments,
                changes.removedDocuments,
                changes.updatedPairs,
                parentInfo,
                organizationTree,
            ),
        )
    }

    private fun createDocument(resource: OrganisasjonselementResource) =
        OrganizationDocument().apply {
            // Scope the MongoDB _id by orgId. The namespaces (afk.no, bfk.no and ofk.no) share one collection, and
            // identifikatorverdi values collide across orgs, so a bare _id let one org's save
            // overwrite another org's document.
            id = "${config.orgid}:${resource.organisasjonsId.identifikatorverdi}"
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

    private fun documentsByOrganizationId(documents: Collection<OrganizationDocument>): Map<String, OrganizationDocument> =
        documents
            .mapNotNull { document -> document.organizationId()?.let { organizationId -> organizationId to document } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (organizationId, documentsWithOrganizationId) ->
                documentsWithOrganizationId.maxWith(
                    compareBy<OrganizationDocument>(
                        { it.hasScopedId(organizationId) },
                        { it.lastModifiedDate ?: LocalDateTime.MIN },
                        { it.id },
                    ),
                )
            }

    private fun extractOrganizationId(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return organizationIdInHref.find(trimmed)?.groupValues?.get(1)
            ?: trimmed.substringAfterLast("/").takeIf { it.isNotBlank() }
    }

    private fun OrganizationDocument.organizationId(): String? =
        data
            ?.organisasjonsId
            ?.identifikatorverdi
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: id.substringAfter(":").takeIf { it.isNotBlank() }

    private fun OrganizationDocument.hasScopedId(organizationId: String): Boolean = id == "$orgId:$organizationId"

    private companion object {
        val organizationIdInHref = Regex("""/organisasjonsid/([^/?#]+)""", RegexOption.IGNORE_CASE)
    }
}
