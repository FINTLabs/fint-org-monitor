package no.fint.orgmonitor.utils

import no.fint.orgmonitor.organization.OrganizationDocument
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
    fun `render highlights changed after values in yellow without red or green column backgrounds`() {
        val before =
            document(
                kortnavn = "OLDK",
                navn = "Old name",
                kode = "OLD_CODE",
                organizationNumber = "111111111",
                overordnet = "parent-a",
                underordnet = listOf("child-a", "child-b"),
            )
        val after =
            document(
                kortnavn = "OLDK",
                navn = "New name",
                kode = "NEW_CODE",
                organizationNumber = "222222222",
                overordnet = "parent-b",
                underordnet = listOf("child-a", "child-c"),
            )

        val html = templateService.render(emptyList(), listOf(before to after), emptyList())

        writeReportPreview(html)

        assertTrue(html.contains("background-color: #fff59d\">New name"))
        assertTrue(html.contains("background-color: #fff59d\">NEW_CODE"))
        assertTrue(html.contains("background-color: #fff59d\">222222222"))
        assertTrue(html.contains("background-color: #fff59d\">\n        Overordnet: <span>parent-b</span>"))
        assertFalse(html.contains("background-color: #fff59d\">Old name"))
        assertFalse(html.contains("background-color: #fff59d\">OLD_CODE"))
        assertFalse(html.contains("background-color: #fff59d\">111111111"))
        assertFalse(html.contains("background-color: #fff59d\">\n        Overordnet: <span>parent-a</span>"))
        assertTrue(html.contains("<td>OLDK</td>"))
        assertFalse(html.contains("#ffb7bd"))
        assertFalse(html.contains("#dbffce"))
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
        kortnavn: String,
        navn: String,
        kode: String = "V40.44.17",
        organizationNumber: String = "974544520",
        overordnet: String = "parent",
        underordnet: List<String> = listOf("child-a", "child-b"),
    ): OrganizationDocument =
        OrganizationDocument(
            id = "fintlabs.no:762",
            orgId = "fintlabs.no",
            data =
                Organisasjonselement().apply {
                    this.kortnavn = kortnavn
                    this.navn = navn
                    organisasjonsKode = Identifikator().apply { identifikatorverdi = kode }
                    organisasjonsId = Identifikator().apply { identifikatorverdi = "762" }
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
