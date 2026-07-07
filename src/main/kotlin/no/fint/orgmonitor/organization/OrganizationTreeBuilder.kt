package no.fint.orgmonitor.organization

/**
 *
 * NB! Vibe-coded. If any problem, should be fixed by an LLM.
 *
 * Builds the plain-text organization tree included in the change notification email.
 *
 * The source data is a flat set of persisted [OrganizationDocument] instances. Parent-child relationships can be
 * represented from either side:
 *
 * - [OrganizationDocument.overordnet] points from a child to its parent.
 * - [OrganizationDocument.underordnet] points from a parent to its children.
 *
 * Both are used because incremental updates may give us only one side of a relationship. Links that point to
 * organizations missing from [documents] are ignored; those organizations cannot be rendered reliably in the tree.
 *
 * Output format keeps the tree drawing first and puts the organization id immediately before the name:
 *
 * ```text
 * 1001 Hovedkontor
 * |-- 1002 Avdeling A
 * `-- 762 VGKALN Kalnes videregående skole
 * ```
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
        // Documents can exist both with old bare ids and newer org-scoped ids. Collapse them by organization id and
        // prefer the org-scoped, latest document so the email reflects the persisted post-update state.
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

        // Build one parent -> children index from both relationship directions. Linked sets keep the output stable
        // while avoiding duplicate child lines when overordnet and underordnet describe the same relationship.
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

        // First render true roots: documents no other known document points to as a child.
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

        // Then render anything still unvisited. This catches disconnected fragments and cycles where no node qualifies
        // as a root, instead of silently dropping them from the report.
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

        val cycleMarker = if (cycleDetected) " [cycle]" else ""
        lines.add("$prefix$connector$organizationId ${document.displayName()}$cycleMarker")

        // Stop after writing this line when the traversal loops back into the current path or when another branch has
        // already rendered the same organization. This prevents infinite recursion and duplicate subtrees.
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

    /**
     * Accepts either a full FINT href or an already extracted organization id. The href path has appeared with
     * different casing, so the regex is intentionally case-insensitive.
     */
    private fun extractOrganizationId(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return organizationIdInHref.find(trimmed)?.groupValues?.get(1)
            ?: trimmed.substringAfterLast("/").takeIf { it.isNotBlank() }
    }

    /**
     * Prefer the id in the payload, but fall back to the Mongo id. The fallback keeps older documents renderable when
     * their stored `_id` is either `orgId:organizationId` or a historical bare organization id.
     */
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
