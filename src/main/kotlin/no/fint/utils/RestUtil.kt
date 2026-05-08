package no.fint.utils

import no.fint.Config
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap

/**
 * Utility class for performing REST operations and tracking last updated timestamps per URI.
 *
 * @property restTemplate the RestTemplate used for HTTP requests
 */
@Component
class RestUtil(
    restTemplateBuilder: RestTemplateBuilder,
    private val config: Config,
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    // TODO: this will not persist between runs of FlaisJob. Needs to be persisted in database
    private val lastUpdatedMap: ConcurrentMap<String, Long> = ConcurrentSkipListMap()

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Fetches updates from the URI set in endpoint config since the last known update timestamp.
     * Updates the internal lastUpdatedMap with the latest timestamp.
     *
     * @param type the type reference for the response body
     * @return the response body of type T
     * @throws IllegalArgumentException if the response body or lastUpdated value is missing
     */
    fun <T> getUpdates(type: ParameterizedTypeReference<T>): T =
        lastUpdatedMap.getOrDefault(config.endpoint, 0L).let { since ->
            logger.info("Fetching since $since")
            // get all OrganisasjonsElement that was updated since timestamp `since`
            val result =
                get(
                    type,
                    UriComponentsBuilder
                        .fromUriString(config.endpoint)
                        .queryParam("sinceTimeStamp", since)
                        .build()
                        .toUriString(),
                )
            // get last-updated timestamp from API
            val lastUpdated =
                requireNotNull(
                    get(
                        // ParameterizedTypeReference is used to get the generic data without an explicit type.
                        object : ParameterizedTypeReference<MutableMap<String, String>>() {},
                        UriComponentsBuilder
                            .fromUriString(config.endpoint)
                            .pathSegment("last-updated")
                            .build()
                            .toUriString(),
                    )["lastUpdated"],
                ) { "No lastUpdated value from ${config.endpoint}" }.toLong()
            lastUpdatedMap[config.endpoint] = lastUpdated
            result
        }

    /**
     * Performs a GET request to the specified URI and returns the response body.
     *
     * @param type the type reference for the response body
     * @param uri the endpoint URI
     * @return the response body of type T
     * @throws IllegalArgumentException if the response body is missing
     */
    fun <T> get(
        type: ParameterizedTypeReference<T>,
        uri: String,
    ): T =
        restTemplate.exchange(uri, HttpMethod.GET, null, type).let { response ->
            logger.info("GET $uri")
            logger.info("Response: ${response.statusCode}")
            requireNotNull(response.body) { "No response body from $uri" }
        }
}
