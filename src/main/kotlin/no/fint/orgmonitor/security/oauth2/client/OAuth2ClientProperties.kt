package no.fint.orgmonitor.security.oauth2.client

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.core.AuthorizationGrantType

// @Validated
@ConfigurationProperties(prefix = "fint.security.oauth2")
class OAuth2ClientProperties {
    var clients: Map<String, OAuth2Client> = emptyMap()

    class OAuth2Client {
        var issuerUri: String? = null
        var tokenUri: String? = null

        @NotBlank
        lateinit var clientId: String

        @NotBlank
        lateinit var clientSecret: String
        lateinit var authorizationGrantType: AuthorizationGrantType
        var scope: Set<String> = emptySet()

        var username: String? = null
        var password: String? = null
    }
}
