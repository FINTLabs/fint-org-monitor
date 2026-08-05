package no.fint.orgmonitor

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import no.fint.orgmonitor.mailing.MailingService
import no.fint.orgmonitor.organization.OrganizationDocument
import no.fint.orgmonitor.organization.OrganizationRepository
import no.fint.orgmonitor.organization.OrganizationService
import no.fint.orgmonitor.sync.SyncStateRepository
import no.fint.orgmonitor.utils.TemplateService
import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrganizationServiceTest(
    @param:Autowired private val organizationRepository: OrganizationRepository,
    @param:Autowired private val organizationService: OrganizationService,
    @param:Autowired private val syncStateRepository: SyncStateRepository,
) : BaseIntegrationTest() {
    @MockitoBean
    private lateinit var mailingService: MailingService

    @MockitoBean
    private lateinit var templateService: TemplateService

    @AfterEach
    fun cleanup() {
        organizationRepository.deleteAll()
        syncStateRepository.deleteAll()
    }

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
                        start = Date.from(Instant.parse("2019-04-01T00:00:00Z"))
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
        // Return the same current source state as the already persisted document.
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "organisasjonsId": {
                                  "identifikatorverdi": "762"
                                },
                                "organisasjonsKode": {
                                  "identifikatorverdi": "V40.44.17"
                                },
                                "organisasjonsnummer": {
                                  "identifikatorverdi": "974544520"
                                },
                                "navn": "VGKALN Kalnes videregående skole",
                                "kortnavn": "VGKALN",
                                "gyldighetsperiode": {
                                  "start": "2019-04-01T00:00:00Z"
                                },
                                "_links": {
                                  "overordnet": [
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/156"
                                    }
                                  ],
                                  "underordnet": [
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/763"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/766"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/770"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/775"
                                    }
                                  ]
                                }
                              }
                            ]
                          },
                          "total_items": 1
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
    fun `should leave local data untouched when current source response is incomplete`() {
        // The repository already contains org 762 from setUp().
        // The source response says there should be 2 items, but only returns 1.
        // That means this is a partial snapshot, so it must not be used for deletion reconciliation.
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(partialCurrentStateResponse()),
                ),
        )

        organizationService.update()

        // If partial snapshots were treated as authoritative, org 762 would be deleted
        // because it is missing from the response.
        assertTrue(organizationRepository.findById("762").isPresent)

        // The returned org 1001 should also not be inserted; the whole reconciliation run is skipped.
        assertFalse(organizationRepository.findById("fintlabs.no:1001").isPresent)
        verify(mailingService, never()).send(anyString())
    }

    @Test
    fun `should create report when there is an update`() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "navn": "Hovedkontor",
                                "kortnavn": "HK",
                                "organisasjonsId": {
                                  "identifikatorverdi": "1001"
                                },
                                "organisasjonsKode": {
                                  "identifikatorverdi": "ORG_1"
                                },
                                "organisasjonsnummer": {
                                  "identifikatorverdi": "999999999"
                                },
                                "gyldighetsperiode": {
                                  "start": "2019-04-01T00:00:00Z"
                                },
                                "_links": {
                                  "self": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001"
                                    }
                                  ],
                                  "overordnet": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1000"
                                    }
                                  ],
                                  "underordnet": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1002"
                                    },
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/762"
                                    }
                                  ]
                                }
                              },
                              {
                                "navn": "Avdeling A",
                                "kortnavn": "AVD_A",
                                "organisasjonsId": {
                                  "identifikatorverdi": "1002"
                                },
                                "gyldighetsperiode": {
                                  "start": "2019-04-01T00:00:00Z"
                                },
                                "organisasjonsKode": {
                                  "identifikatorverdi": "ORG_2"
                                },
                                "organisasjonsnummer": {
                                  "identifikatorverdi": "888888888"
                                },
                                "_links": {
                                  "self": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1002"
                                    }
                                  ],
                                  "overordnet": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001"
                                    }
                                  ]
                                }
                              },
                              {
                                "navn": "VGKALN Kalnes videregående skole",
                                "kortnavn": "VGKALN",
                                "organisasjonsId": {
                                  "identifikatorverdi": "762"
                                },
                                "gyldighetsperiode": {
                                  "start": "2019-04-01T00:00:00Z"
                                },
                                "organisasjonsKode": {
                                  "identifikatorverdi": "ORG_2"
                                },
                                "organisasjonsnummer": {
                                  "identifikatorverdi": "888888888"
                                },
                                "_links": {
                                  "self": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1002"
                                    }
                                  ],
                                  "overordnet": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001"
                                    }
                                  ],
                                  "underordnet": [
                                    {
                                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1003"
                                    }
                                  ]
                                }
                              }
                            ]
                          },
                          "total_items": 3,
                          "_links": {
                            "self": [
                              {
                                "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement"
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        var organizationTree = ""
        `when`(templateService.render(anyList(), anyList(), anyList(), anyList(), anyString()))
            .thenAnswer { invocation ->
                organizationTree = invocation.getArgument(4)
                "<html>Default Mock HTML</html>"
            }
        organizationService.update()
        verify(templateService).render(anyList(), anyList(), anyList(), anyList(), anyString())
        assertTrue(
            organizationTree.contains(
                """
                1001 Hovedkontor
                |-- 1002 Avdeling A
                `-- 762 VGKALN Kalnes videregående skole
                """.trimIndent(),
            ),
            organizationTree,
        )
    }

    @Test
    fun `should persist new documents with their id and not re-report them on a second run`() {
        val stub =
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "navn": "Hovedkontor",
                                "kortnavn": "HK",
                                "organisasjonsId": { "identifikatorverdi": "1001" },
                                "organisasjonsKode": { "identifikatorverdi": "ORG_1" },
                                "organisasjonsnummer": { "identifikatorverdi": "999999999" },
                                "gyldighetsperiode": { "start": "2019-04-01T00:00:00Z" },
                                "_links": {
                                  "self": [ { "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001" } ]
                                }
                              },
                              {
                                "navn": "Avdeling A",
                                "kortnavn": "AVD_A",
                                "organisasjonsId": { "identifikatorverdi": "1002" },
                                "organisasjonsKode": { "identifikatorverdi": "ORG_2" },
                                "organisasjonsnummer": { "identifikatorverdi": "888888888" },
                                "gyldighetsperiode": { "start": "2019-04-01T00:00:00Z" },
                                "_links": {
                                  "self": [ { "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1002" } ]
                                }
                              }
                            ]
                          },
                          "total_items": 2
                        }
                        """.trimIndent(),
                    ),
                )
        wireMockServer.stubFor(stub)
        `when`(templateService.render(anyList(), anyList(), anyList(), anyList(), anyString()))
            .thenReturn("<html>Default Mock HTML</html>")

        // First run: both elements are new and must be added.
        organizationService.update()

        // Both new docs persisted under their own id (regression: previously saved with empty id and collided).
        assert(organizationRepository.findById("fintlabs.no:1001").isPresent) { "Document 1001 was not persisted" }
        assert(organizationRepository.findById("fintlabs.no:1002").isPresent) { "Document 1002 was not persisted" }

        // Second run with identical data: nothing changed, so no new notification.
        organizationService.update()

        verify(mailingService, times(1)).send(anyString())
    }

    @Test
    fun `update for one org does not override another org's document with the same identifikatorverdi`() {
        // Seed a document belonging to a DIFFERENT org under the colliding bare identifikatorverdi.
        // afk.no and bfk.no share one collection and have overlapping identifikatorverdi values, so a
        // bare _id let one org's saveAll() upsert overwrite the other org's document. We seed at the
        // bare id "1001" on purpose: it is the exact _id the buggy code computes, so a regression back
        // to a bare _id would upsert over this document and fail the assertions below.
        val otherOrgElement =
            Organisasjonselement().apply {
                organisasjonsId = Identifikator().apply { identifikatorverdi = "1001" }
                organisasjonsKode = Identifikator().apply { identifikatorverdi = "OTHER" }
                organisasjonsnummer = Identifikator().apply { identifikatorverdi = "111111111" }
                navn = "Annen org enhet"
                kortnavn = "ANNEN"
                gyldighetsperiode =
                    Periode().apply { start = Date.from(Instant.parse("2019-04-01T00:00:00Z")) }
            }
        organizationRepository.save(
            OrganizationDocument(
                id = "1001",
                orgId = "annen.no",
                data = otherOrgElement,
            ),
        )

        // fintlabs.no (the test profile's orgid) receives an update for the same identifikatorverdi.
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "navn": "Hovedkontor",
                                "kortnavn": "HK",
                                "organisasjonsId": { "identifikatorverdi": "1001" },
                                "organisasjonsKode": { "identifikatorverdi": "ORG_1" },
                                "organisasjonsnummer": { "identifikatorverdi": "999999999" },
                                "gyldighetsperiode": { "start": "2019-04-01T00:00:00Z" },
                                "_links": {
                                  "self": [ { "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001" } ]
                                }
                              }
                            ]
                          },
                          "total_items": 1
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        `when`(templateService.render(anyList(), anyList(), anyList(), anyList(), anyString()))
            .thenReturn("<html>Default Mock HTML</html>")

        organizationService.update()

        // The other org's document survives untouched: still present, still its org, still its data.
        val otherDoc = organizationRepository.findById("1001")
        assert(otherDoc.isPresent) { "annen.no document was overwritten/removed by the fintlabs.no update" }
        assert(otherDoc.get().orgId == "annen.no") { "annen.no document's orgId was flipped to ${otherDoc.get().orgId}" }
        assert(otherDoc.get().data?.navn == "Annen org enhet") { "annen.no document's data was overwritten" }

        // fintlabs.no got its own org-scoped document.
        val fintlabsDoc = organizationRepository.findById("fintlabs.no:1001")
        assert(fintlabsDoc.isPresent) { "fintlabs.no document was not persisted under its org-scoped id" }
        assert(fintlabsDoc.get().orgId == "fintlabs.no") { "fintlabs.no document has wrong orgId" }
    }

    @Test
    fun `should not do anything when the order of underordnet changes`() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "organisasjonsId": {
                                  "identifikatorverdi": "762"
                                },
                                "organisasjonsKode": {
                                  "identifikatorverdi": "V40.44.17"
                                },
                                "organisasjonsnummer": {
                                  "identifikatorverdi": "974544520"
                                },
                                "navn": "VGKALN Kalnes videregående skole",
                                "kortnavn": "VGKALN",
                                "gyldighetsperiode": {
                                  "start": "2019-04-01T00:00:00Z"
                                },
                                "_links": {
                                  "overordnet": [
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/156"
                                    }
                                  ],
                                  "underordnet": [
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/775"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/763"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/770"
                                    },
                                    {
                                      "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/766"
                                    }
                                  ]
                                }
                              }
                            ]
                          },
                          "total_items": 1
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        organizationService.update()
        verify(mailingService, never()).send(anyString())
    }

    @Test
    fun `should delete stale documents missing from current source and exclude them from organization tree`() {
        organizationRepository.deleteAll()
        organizationRepository.saveAll(
            listOf(
                document(
                    organizationId = "120",
                    name = "VGBLEI Bleiker videregående skole",
                    kortnavn = "VGBLEI",
                    children = listOf(href("570")),
                ),
                document(
                    organizationId = "570",
                    name = "VGBLEI pedagogisk ledelse",
                    kortnavn = "VGBLEI",
                    parent = href("120"),
                ),
                document(
                    organizationId = "663",
                    name = "VGVEST grunnskole",
                    kortnavn = "VGVEST grunnsk",
                    children = listOf(href("664")),
                ),
                document(
                    organizationId = "664",
                    name = "VGVEST drift/renhold",
                    kortnavn = "VGVEST",
                    parent = href("663"),
                ),
            ),
        )

        wireMockServer.stubFor(
            get(urlPathEqualTo("/administrasjon/organisasjon/organisasjonselement"))
                .willReturn(
                    okJson(
                        """
                        {
                          "_embedded": {
                            "_entries": [
                              {
                                "navn": "VGBLEI Bleiker videregående skole",
                                "kortnavn": "VGBLEI",
                                "organisasjonsId": { "identifikatorverdi": "120" },
                                "organisasjonsKode": { "identifikatorverdi": "F40.42.52" },
                                "organisasjonsnummer": { "identifikatorverdi": "974587831" },
                                "gyldighetsperiode": { "start": "2023-01-01T00:00:00Z" },
                                "_links": {
                                  "overordnet": [ { "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/36" } ],
                                  "underordnet": []
                                }
                              },
                              {
                                "navn": "VGVEST grunnskole",
                                "kortnavn": "VGVEST grunnsk",
                                "organisasjonsId": { "identifikatorverdi": "663" },
                                "organisasjonsKode": { "identifikatorverdi": "F40.43.74.65" },
                                "organisasjonsnummer": { "identifikatorverdi": "974587815" },
                                "gyldighetsperiode": { "start": "2025-03-24T00:00:00Z" },
                                "_links": {
                                  "overordnet": [ { "href": "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/622" } ],
                                  "underordnet": []
                                }
                              }
                            ]
                          },
                          "total_items": 2
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        var removedDocuments: List<OrganizationDocument> = emptyList()
        var organizationTree = ""
        `when`(templateService.render(anyList(), anyList(), anyList(), anyList(), anyString()))
            .thenAnswer { invocation ->
                removedDocuments = invocation.getArgument(1)
                organizationTree = invocation.getArgument(4)
                "<html>Default Mock HTML</html>"
            }

        organizationService.update()

        assertFalse(organizationRepository.findById("fintlabs.no:570").isPresent)
        assertFalse(organizationRepository.findById("fintlabs.no:664").isPresent)
        assertTrue(removedDocuments.map { it.data?.organisasjonsId?.identifikatorverdi }.containsAll(listOf("570", "664")))
        assertFalse(organizationTree.contains("570 VGBLEI pedagogisk ledelse"), organizationTree)
        assertFalse(organizationTree.contains("664 VGVEST drift/renhold"), organizationTree)
    }

    private fun document(
        organizationId: String,
        name: String,
        kortnavn: String,
        parent: String? = null,
        children: List<String> = emptyList(),
    ): OrganizationDocument =
        OrganizationDocument(
            id = "fintlabs.no:$organizationId",
            orgId = "fintlabs.no",
            data =
                Organisasjonselement().apply {
                    organisasjonsId = Identifikator().apply { identifikatorverdi = organizationId }
                    organisasjonsKode = Identifikator().apply { identifikatorverdi = "ORG_$organizationId" }
                    organisasjonsnummer = Identifikator().apply { identifikatorverdi = "999999999" }
                    navn = name
                    this.kortnavn = kortnavn
                    gyldighetsperiode =
                        Periode().apply {
                            start = Date.from(Instant.parse("2019-04-01T00:00:00Z"))
                        }
                },
            overordnet = parent,
            underordnet = children,
        )

    private fun href(organizationId: String): String =
        "https://test.felleskomponent.no/administrasjon/organisasjon/organisasjonselement/organisasjonsId/$organizationId"

    private fun partialCurrentStateResponse(): String =
        """
        {
          "_embedded": {
            "_entries": [
              {
                "navn": "Hovedkontor",
                "kortnavn": "HK",
                "organisasjonsId": { "identifikatorverdi": "1001" },
                "organisasjonsKode": { "identifikatorverdi": "ORG_1" },
                "organisasjonsnummer": { "identifikatorverdi": "999999999" },
                "gyldighetsperiode": { "start": "2019-04-01T00:00:00Z" },
                "_links": {
                  "self": [
                    {
                      "href": "https://test.fintlabs.no/administrasjon/organisasjon/organisasjonselement/organisasjonsid/1001"
                    }
                  ]
                }
              }
            ]
          },
          "total_items": 2
        }
        """.trimIndent()
}
