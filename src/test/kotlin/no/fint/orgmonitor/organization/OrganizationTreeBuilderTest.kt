package no.fint.orgmonitor.organization

import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OrganizationTreeBuilderTest {
    @Test
    fun `build creates unix-style tree with names and organization ids`() {
        val tree =
            OrganizationTreeBuilder.build(
                listOf(
                    document(
                        organizationId = "100",
                        name = "Root",
                        children = listOf(href("200"), href("300")),
                    ),
                    document(
                        organizationId = "200",
                        name = "Child A",
                        parent = href("100"),
                        children = listOf(href("400")),
                    ),
                    document(
                        organizationId = "300",
                        name = "Child B",
                        parent = href("100"),
                    ),
                    document(
                        organizationId = "400",
                        name = "Grandchild",
                        parent = href("200"),
                    ),
                ),
            )

        assertEquals(
            """
            100 Root
            |-- 200 Child A
            |   `-- 400 Grandchild
            `-- 300 Child B
            """.trimIndent(),
            tree,
        )
    }

    @Test
    fun `build includes disconnected roots`() {
        val tree =
            OrganizationTreeBuilder.build(
                listOf(
                    document(organizationId = "200", name = "Second root"),
                    document(organizationId = "100", name = "First root"),
                ),
            )

        assertEquals(
            """
            100 First root
            200 Second root
            """.trimIndent(),
            tree,
        )
    }

    @Test
    fun `build prefers org-scoped duplicate document so tree shows updated state`() {
        val tree =
            OrganizationTreeBuilder.build(
                listOf(
                    document(
                        id = "762",
                        organizationId = "762",
                        name = "School",
                        parent = href("156"),
                    ),
                    document(
                        organizationId = "1001",
                        name = "Root",
                        children = listOf(href("762")),
                    ),
                    document(
                        id = "fintlabs.no:762",
                        organizationId = "762",
                        name = "School",
                        parent = href("1001"),
                    ),
                ),
            )

        assertEquals(
            """
            1001 Root
            `-- 762 School
            """.trimIndent(),
            tree,
        )
    }

    @Test
    fun `build does not attach child from overordnet when parent does not list child`() {
        val tree =
            OrganizationTreeBuilder.build(
                listOf(
                    document(organizationId = "100", name = "Parent"),
                    document(
                        organizationId = "200",
                        name = "Stale child",
                        parent = href("100"),
                    ),
                ),
            )

        assertEquals(
            """
            100 Parent
            200 Stale child
            """.trimIndent(),
            tree,
        )
    }

    private fun document(
        id: String? = null,
        organizationId: String,
        name: String,
        parent: String? = null,
        children: List<String> = emptyList(),
    ): OrganizationDocument =
        OrganizationDocument(
            id = id ?: "fintlabs.no:$organizationId",
            orgId = "fintlabs.no",
            data =
                Organisasjonselement().apply {
                    organisasjonsId = Identifikator().apply { identifikatorverdi = organizationId }
                    navn = name
                },
            overordnet = parent,
            underordnet = children,
        )

    private fun href(organizationId: String): String =
        "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/$organizationId"
}
