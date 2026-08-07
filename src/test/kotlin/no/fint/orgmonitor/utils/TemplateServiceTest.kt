package no.fint.orgmonitor.utils

import no.fint.orgmonitor.organization.OrganizationDocument
import no.fint.orgmonitor.organization.SimpleOrganizationInfo
import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import org.junit.jupiter.api.Test
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateServiceTest {
    private val templateService = TemplateService(templateEngine())

    @Test
    fun `render writes preview with added removed and changed organizations`() {
        val added =
            document(
                organizationId = "1001",
                kortnavn = "NEW",
                navn = "New org",
            )
        val removed =
            document(
                organizationId = "1002",
                kortnavn = "DEL",
                navn = "Deleted org",
            )
        val before =
            document(
                organizationId = "762",
                kortnavn = "OLDK",
                navn = "Old name",
                kode = "OLD_CODE",
                organizationNumber = "111111111",
                overordnet = "parent-a",
                underordnet = listOf("child-a", "child-b"),
            )
        val after =
            document(
                organizationId = "762",
                kortnavn = "OLDK",
                navn = "New name",
                kode = "NEW_CODE",
                organizationNumber = "222222222",
                overordnet = "parent-b",
                underordnet = listOf("child-a", "child-c"),
            )

        val html = templateService.render(listOf(added), listOf(removed), listOf(before to after), emptyList(), SAMPLE_ORGANIZATION_TREE)

        writeReportPreview(html)

        assertTrue(html.contains("Følgende organisasjonselementer er nye:"))
        assertTrue(html.contains("New org"))
        assertTrue(html.contains("Følgende organisasjonselementer er fjernet:"))
        assertTrue(html.contains("Deleted org"))
        assertTrue(html.contains("Følgende organisasjonselementer er endret:"))
        assertTrue(html.contains("background-color: #fff59d\">New name"))
        assertTrue(html.contains("background-color: #fff59d\">NEW_CODE"))
        assertTrue(html.contains("background-color: #fff59d\">222222222"))
        assertTrue(html.contains("background-color: #fff59d\">\n        Overordnet: <span>parent-b</span>"))
        assertFalse(html.contains("background-color: #fff59d\">Old name"))
        assertFalse(html.contains("background-color: #fff59d\">OLD_CODE"))
        assertFalse(html.contains("background-color: #fff59d\">111111111"))
        assertFalse(html.contains("background-color: #fff59d\">\n        Overordnet: <span>parent-a</span>"))
        assertTrue(html.contains("<td>OLDK</td>"))
        assertTrue(html.contains("100 Root"))
        assertFalse(html.contains("#ffb7bd"))
        assertFalse(html.contains("#dbffce"))
    }

    @Test
    fun `render includes organization tree`() {
        val html = templateService.render(emptyList(), emptyList(), emptyList(), emptyList(), SAMPLE_ORGANIZATION_TREE)

        assertTrue(html.contains("<h2>Organisasjonstre:</h2>"))
        assertTrue(html.contains("100 Root"))
        assertTrue(html.contains("`-- 200 Child"))
    }

    @Test
    fun `render includes removed organizations`() {
        val removed = document(kortnavn = "OLD", navn = "Removed org")

        val html = templateService.render(emptyList(), listOf(removed), emptyList(), emptyList(), "")

        assertTrue(html.contains("Følgende organisasjonselementer er fjernet:"))
        assertTrue(html.contains("Removed org"))
    }

    @Test
    fun `render includes parent organization info`() {
        val parentInfo = listOf(SimpleOrganizationInfo("fintlabs.no:123", "Parent org"))

        val html = templateService.render(emptyList(), emptyList(), emptyList(), parentInfo, "")

        assertTrue(html.contains("Overordnede organisasjonsenheter:"))
        assertTrue(html.contains("<td>fintlabs.no:123</td>"))
        assertTrue(html.contains("<td>Parent org</td>"))
    }

    private companion object {
        const val SAMPLE_ORGANIZATION_TREE = "100 Root\n`-- 200 Child"
    }

    private fun templateEngine(): SpringTemplateEngine {
        val resolver =
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                templateMode = TemplateMode.HTML
                characterEncoding = "UTF-8"
                isCacheable = false
            }

        return SpringTemplateEngine().apply {
            setTemplateResolver(resolver)
        }
    }

    private fun writeReportPreview(html: String) {
        val output = Path.of("build/reports/org-monitor/email-report.html")
        Files.createDirectories(output.parent)
        Files.writeString(output, html)
    }

    private fun document(
        organizationId: String = "762",
        kortnavn: String,
        navn: String,
        kode: String = "V40.44.17",
        organizationNumber: String = "974544520",
        overordnet: String = "parent",
        underordnet: List<String> = listOf("child-a", "child-b"),
    ): OrganizationDocument =
        OrganizationDocument(
            id = "fintlabs.no:$organizationId",
            orgId = "fintlabs.no",
            data =
                Organisasjonselement().apply {
                    this.kortnavn = kortnavn
                    this.navn = navn
                    organisasjonsKode = Identifikator().apply { identifikatorverdi = kode }
                    organisasjonsId = Identifikator().apply { identifikatorverdi = organizationId }
                    organisasjonsnummer = Identifikator().apply { identifikatorverdi = organizationNumber }
                    gyldighetsperiode =
                        Periode().apply {
                            start = Date.from(Instant.parse("2019-04-01T00:00:00Z"))
                        }
                },
            overordnet = overordnet,
            underordnet = underordnet,
        )
}
