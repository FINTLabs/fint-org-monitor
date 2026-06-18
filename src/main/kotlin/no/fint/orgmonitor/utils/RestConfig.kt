package no.fint.orgmonitor.utils

import no.fint.orgmonitor.security.oauth2.client.OAuth2ClientInterceptor
import no.fint.orgmonitor.security.oauth2.client.OAuth2ClientRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.client.RestClient

// Taken from https://github.com/FINTLabs/flais-idp-gateway
@Configuration
@EnableCaching
class RestConfig(
    private val clientRepository: OAuth2ClientRepository,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
) {
    @Bean
    fun idpClient(
        @Value("\${fint.core.idp.url}") idpUrl: String,
        @Value("\${fint.core.idp.oauth-client}") oauthClient: String,
    ): RestClient {
        val clientRegistration = clientRepository.findByRegistrationId(oauthClient)
        val authInitializer = OAuth2ClientInterceptor(authorizedClientManager, clientRegistration)
        return RestClient
            .builder()
            .requestInterceptor(authInitializer)
            .baseUrl(idpUrl)
            .defaultHeaders {
                it.accept = listOf(MediaType.APPLICATION_JSON)
                it.contentType = MediaType.APPLICATION_JSON
            }.build()
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                ConcurrentMapCache("application-registration"),
            ),
        )
        return cacheManager
    }
}
