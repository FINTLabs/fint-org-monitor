package no.fint.orgmonitor

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestClient

/**
 * Replaces the real OAuth2-enabled `idpClient` with a plain [RestClient] so tests do not
 * attempt to fetch a token. `RestUtil` calls `.uri(absoluteUrl)` from `fint.orgmonitor.endpoint`
 * (WireMock), so no baseUrl or interceptor is needed.
 */
@TestConfiguration
class TestRestConfig {
    @Bean
    @Primary
    fun testIdpClient(): RestClient = RestClient.builder().build()
}
