package no.fint.utils

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
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()
    private val lastUpdatedMap: ConcurrentMap<String, Long> = ConcurrentSkipListMap()

    private val logger = LoggerFactory.getLogger(RestUtil::class.java)

    /**
     * Fetches updates from the given URI since the last known update timestamp.
     * Updates the internal lastUpdatedMap with the latest timestamp.
     *
     * @param type the type reference for the response body
     * @param uri the endpoint URI
     * @return the response body of type T
     * @throws IllegalArgumentException if the response body or lastUpdated value is missing
     */
    fun <T> getUpdates(
        type: ParameterizedTypeReference<T>,
        uri: String,
    ): T =
        lastUpdatedMap.getOrDefault(uri, 0L).let { since ->
            logger.info("Fetching $uri since $since")
            // get all OrganisasjonsElement that was updated since timestamp `since`
            val result =
                get(
                    type,
                    UriComponentsBuilder
                        .fromUriString(uri)
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
                            .fromUriString(uri)
                            .pathSegment("last-updated")
                            .build()
                            .toUriString(),
                    )["lastUpdated"],
                ) { "No lastUpdated value from $uri" }.toLong()
            lastUpdatedMap[uri] = lastUpdated
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
