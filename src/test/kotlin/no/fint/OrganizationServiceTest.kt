package no.fint

import no.fint.organization.OrganizationDocument
import no.fint.organization.OrganizationRepository
import no.fint.organization.OrganizationService
import no.fint.utils.RestUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrganizationServiceTest(
    @Autowired private val organizationRepository: OrganizationRepository,
    @Autowired private val organizationService: OrganizationService,
) : BaseIntegrationTest() {
    @BeforeEach
    fun setUp() {
        organizationRepository.save(
            OrganizationDocument(
                id = "123",
                orgId = "viken.no",
                data = null,
            ),
        )
    }

    @Test
    fun `should not do anything when there is no update`() {
        organizationService.update()
        // assert nothing has changed in database
        val orgs = organizationRepository.findAll()
        assert(orgs.size == 1)
    }

    @Test
    fun `debug wiremock response`() {
        val restTemplate =
            org.springframework.web.client
                .RestTemplate()
        val baseUrl = BaseIntegrationTest.wireMockServer.baseUrl()

        println("--- DEBUG WIREMOCK START ---")
        try {
            // Check the main list endpoint
            val listUrl = "$baseUrl/administrasjon/organisasjon/organisasjonselement"
            val listResponse = restTemplate.getForObject(listUrl, String::class.java)
            println("GET $listUrl\nResponse: $listResponse")
        } catch (e: Exception) {
            println("Main Endpoint Error: $e")
        }

        try {
            // Check the main list endpoint
            val listUrl = "$baseUrl/administrasjon/organisasjon/organisasjonselement?sinceTimeStamp=0"
            val listResponse = restTemplate.getForObject(listUrl, String::class.java)
            println("GET $listUrl\nResponse: $listResponse")
        } catch (e: Exception) {
            println("Main Endpoint Error: $e")
        }

        try {
            // Check the last-updated endpoint
            val lastUpdatedUrl = "$baseUrl/administrasjon/organisasjon/organisasjonselement/last-updated"
            val lastUpdatedResponse = restTemplate.getForObject(lastUpdatedUrl, String::class.java)
            println("GET $lastUpdatedUrl\nResponse: $lastUpdatedResponse")
        } catch (e: Exception) {
            println("Last Updated Error: $e")
        }
        println("--- DEBUG WIREMOCK END ---")
    }

    @Test
    fun `should create report when there is one update`() {
        assert(true)
    }

    @Test
    fun `should create report when there is several updates`() {
        assert(true)
    }
}
