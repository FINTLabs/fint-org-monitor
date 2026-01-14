package no.fint

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import no.fint.mailing.MailingService
import no.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.organization.OrganizationDocument
import no.fint.organization.OrganizationRepository
import no.fint.organization.OrganizationService
import no.fint.utils.TemplateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Date

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional // To reset database before each test.
class OrganizationServiceTest(
    @Autowired private val organizationRepository: OrganizationRepository,
    @Autowired private val organizationService: OrganizationService,
) : BaseIntegrationTest() {
    @MockitoBean
    private lateinit var mailingService: MailingService

    @MockitoBean
    private lateinit var templateService: TemplateService

    @BeforeEach
    fun setUp() {
        // Mock mailingservice
        `when`(mailingService.send(anyString())).thenReturn(true)

        // Save one entry to the test database
        val organisasjonselement =
            Organisasjonselement().apply {
                organisasjonsId = Identifikator().apply { identifikatorverdi = "762" }
                organisasjonsKode = Identifikator().apply { identifikatorverdi = "V40.44.17" }
                organisasjonsnummer = Identifikator().apply { identifikatorverdi = "974544520" }
                navn = "VGKALN Kalnes videregående skole"
                kortnavn = "VGKALN"
                gyldighetsperiode =
                    Periode().apply {
                        start = Date.from(java.time.Instant.parse("2019-04-01T00:00:00Z"))
                    }
            }

        organizationRepository.save(
            OrganizationDocument(
                id = "762",
                orgId = "fintlabs.no",
                data = organisasjonselement,
                overordnet = "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/156",
                underordnet =
                    listOf(
                        "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/763",
                        "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/766",
                        "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/770",
                        "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/775",
                    ),
            ),
        )
    }

    @Test
    fun `should not do anything when there is no update`() {
        // Override WireMock mappings to return an empty list (no updates)
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .withQueryParam("sinceTimeStamp", matching("\\d+"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": []
                          },
                          "total_items": 0,
                          "content": []
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        organizationService.update()
        // assert nothing has changed in database
        val orgs = organizationRepository.findAll()
        assert(orgs.size == 1)
    }

    @Test
    fun `should create report when there is an update`() {
        `when`(templateService.render(anyList(), anyList(), anyList())).thenReturn("<html>Default Mock HTML</html>")
        organizationService.update()
        verify(templateService).render(anyList(), anyList(), anyList())
    }
}
