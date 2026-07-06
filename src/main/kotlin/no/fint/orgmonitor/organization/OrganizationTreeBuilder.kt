package no.fint.orgmonitor.organization

/**
 * Entirely vibe-coded. Use LLM to fix if any problem.
 * Used to generate a Tree like structure of the whole Organization.
 *
 */

object OrganizationTreeBuilder {
    private val organizationIdInHref = Regex("""/organisasjonsid/([^/?#]+)""", RegexOption.IGNORE_CASE)
    private val documentComparator =
        compareBy<OrganizationDocument>(
            {
                it.data
                    ?.navn
                    .orEmpty()
                    .lowercase()
            },
            { it.organizationId().orEmpty() },
        )

    fun build(documents: Collection<OrganizationDocument>): String {
        val documentsByOrganizationId =
            documents
                .mapNotNull { document -> document.organizationId()?.let { it to document } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (organizationId, documentsWithOrganizationId) ->
                    documentsWithOrganizationId.maxWith(
                        compareBy<OrganizationDocument>(
                            { it.hasScopedId(organizationId) },
                            { it.lastModifiedDate ?: java.time.LocalDateTime.MIN },
                            { it.id },
                        ),
                    )
                }

        if (documentsByOrganizationId.isEmpty()) {
            return ""
        }

        val childrenByParentId = linkedMapOf<String, LinkedHashSet<String>>()
        val knownChildIds = linkedSetOf<String>()

        documentsByOrganizationId.forEach { (organizationId, document) ->
            extractOrganizationId(document.overordnet)
                ?.takeIf { it in documentsByOrganizationId && it != organizationId }
                ?.let { parentId ->
                    childrenByParentId.getOrPut(parentId) { linkedSetOf() }.add(organizationId)
                    knownChildIds.add(organizationId)
                }

            document.underordnet
                .orEmpty()
                .mapNotNull(::extractOrganizationId)
                .filter { it in documentsByOrganizationId && it != organizationId }
                .forEach { childId ->
                    childrenByParentId.getOrPut(organizationId) { linkedSetOf() }.add(childId)
                    knownChildIds.add(childId)
                }
        }

        val lines = mutableListOf<String>()
        val visited = linkedSetOf<String>()

        documentsByOrganizationId.values
            .filter { it.organizationId() !in knownChildIds }
            .sortedWith(documentComparator)
            .forEach { document ->
                appendTreeLines(
                    document = document,
                    prefix = "",
                    isLast = true,
                    isRoot = true,
                    childrenByParentId = childrenByParentId,
                    documentsByOrganizationId = documentsByOrganizationId,
                    visited = visited,
                    path = emptySet(),
                    lines = lines,
                )
            }

        documentsByOrganizationId.values
            .filter { it.organizationId() !in visited }
            .sortedWith(documentComparator)
            .forEach { document ->
                appendTreeLines(
                    document = document,
                    prefix = "",
                    isLast = true,
                    isRoot = true,
                    childrenByParentId = childrenByParentId,
                    documentsByOrganizationId = documentsByOrganizationId,
                    visited = visited,
                    path = emptySet(),
                    lines = lines,
                )
            }

        return lines.joinToString("\n")
    }

    private fun appendTreeLines(
        document: OrganizationDocument,
        prefix: String,
        isLast: Boolean,
        isRoot: Boolean,
        childrenByParentId: Map<String, Set<String>>,
        documentsByOrganizationId: Map<String, OrganizationDocument>,
        visited: MutableSet<String>,
        path: Set<String>,
        lines: MutableList<String>,
    ) {
        val organizationId = document.organizationId() ?: return
        val cycleDetected = organizationId in path
        val connector =
            when {
                isRoot -> ""
                isLast -> "`-- "
                else -> "|-- "
            }

        lines.add("$prefix$connector${document.displayName()} ($organizationId)${if (cycleDetected) " [cycle]" else ""}")

        if (cycleDetected || !visited.add(organizationId)) {
            return
        }

        val childPrefix =
            when {
                isRoot -> ""
                isLast -> "$prefix    "
                else -> "$prefix|   "
            }
        val children =
            childrenByParentId[organizationId]
                .orEmpty()
                .mapNotNull(documentsByOrganizationId::get)
                .sortedWith(documentComparator)

        children.forEachIndexed { index, child ->
            appendTreeLines(
                document = child,
                prefix = childPrefix,
                isLast = index == children.lastIndex,
                isRoot = false,
                childrenByParentId = childrenByParentId,
                documentsByOrganizationId = documentsByOrganizationId,
                visited = visited,
                path = path + organizationId,
                lines = lines,
            )
        }
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

    private fun OrganizationDocument.displayName(): String = data?.navn?.trim()?.takeIf { it.isNotEmpty() } ?: "(uten navn)"
}
