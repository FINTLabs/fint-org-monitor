package no.fint.orgmonitor.security.oauth2.client

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.core.AuthorizationGrantType

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties::class)
class OAuth2ClientConfig {
    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: OAuth2ClientRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder
                .builder()
                .authorizationCode()
                .clientCredentials()
                .password()
                .refreshToken()
                .build()

        return AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService,
        ).apply {
            setAuthorizedClientProvider(authorizedClientProvider)
            setContextAttributesMapper {
                val config = clientRegistrationRepository.getConfigByRegistrationId(it.clientRegistrationId)

                when (config.authorizationGrantType) {
                    AuthorizationGrantType.PASSWORD -> {
                        mapOf(
                            OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME to config.username,
                            OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME to config.password,
                        )
                    }

                    else -> {
                        emptyMap()
                    }
                }
            }
        }
    }
}
