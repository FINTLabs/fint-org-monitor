package no.fint

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

@SpringBootTest
abstract class BaseIntegrationTest {
    companion object {
        lateinit var wireMockServer: WireMockServer

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
            wireMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMockServer.stop()
        }

        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("fint.orgmonitor.endpoint") { "${wireMockServer.baseUrl()}/administrasjon/organisasjon/organisasjonselement" }
            registry.add("fint.database.url") { postgres.jdbcUrl }
            registry.add("fint.database.username") { postgres.username }
            registry.add("fint.database.password") { postgres.password }
        }
    }
}
