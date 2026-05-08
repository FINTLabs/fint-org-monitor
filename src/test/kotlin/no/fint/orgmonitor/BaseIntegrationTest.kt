package no.fint.orgmonitor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
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
        val mongodb: MongoDBContainer = MongoDBContainer("mongo:7")

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("fint.orgmonitor.endpoint") { "${wireMockServer.baseUrl()}/administrasjon/organisasjon/organisasjonselement" }
        }
    }
}
