package no.fint.orgmonitor.security.oauth2.client

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.stereotype.Component

@Component
class OAuth2ClientRepository(
    private val properties: OAuth2ClientProperties,
) : ClientRegistrationRepository {
    override fun findByRegistrationId(registrationId: String): ClientRegistration {
        val clientRegistrationConfig = getConfigByRegistrationId(registrationId)
        val builder = getClientRegistrationBuilder(registrationId, clientRegistrationConfig)

        return builder
            .clientId(clientRegistrationConfig.clientId)
            .clientSecret(clientRegistrationConfig.clientSecret)
            .scope(clientRegistrationConfig.scope)
            .authorizationGrantType(clientRegistrationConfig.authorizationGrantType)
            .build()
    }

    fun getConfigByRegistrationId(registrationId: String) =
        properties.clients[registrationId] ?: throw ClientNotFoundException("client $registrationId not found in configuration")

    private fun getClientRegistrationBuilder(
        name: String,
        clientRegistrationConfig: OAuth2ClientProperties.OAuth2Client,
    ) = when {
        clientRegistrationConfig.issuerUri.isNullOrBlank().not() -> {
            ClientRegistrations
                .fromOidcIssuerLocation(
                    clientRegistrationConfig.issuerUri,
                ).registrationId(name)
        }

        clientRegistrationConfig.tokenUri.isNullOrBlank().not() -> {
            ClientRegistration
                .withRegistrationId(
                    name,
                ).tokenUri(clientRegistrationConfig.tokenUri)
        }

        else -> {
            throw IllegalArgumentException("issuerUri or tokenUri must be set")
        }
    }
}
