package no.fint.orgmonitor.security.oauth2.client

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInitializer
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.util.Assert
import java.io.IOException

class OAuth2ClientInterceptor(
    private val manager: OAuth2AuthorizedClientManager,
    private val clientRegistration: ClientRegistration,
) : ClientHttpRequestInterceptor,
    ClientHttpRequestInitializer {
    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.setBearerAuth(getToken())
        return execution.execute(request, body)
    }

    override fun initialize(request: ClientHttpRequest) {
        request.headers.setBearerAuth(getToken())
    }

    private fun getToken(): String {
        val oAuth2AuthorizeRequest =
            OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistration.registrationId)
                .principal(clientRegistration.clientId)
                .build()

        val client = manager.authorize(oAuth2AuthorizeRequest)
        Assert.notNull(client) {
            "Authorized client failed for Registration id: '" + clientRegistration.registrationId +
                "', returned client is null"
        }
        return client!!.accessToken.tokenValue
    }
}
