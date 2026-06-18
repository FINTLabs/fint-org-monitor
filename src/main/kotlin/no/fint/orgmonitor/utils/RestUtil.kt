package no.fint.orgmonitor.utils

import no.fint.orgmonitor.Config
import no.fint.orgmonitor.sync.SyncStateRepository
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

/**
 * Utility class for performing REST operations.
 *
 * The last-seen update timestamp is read from the database (persisted per orgId) so that it
 * survives restarts. This class only reads it; the caller persists the new timestamp once an
 * update has been processed successfully.
 */
@Component
class RestUtil(
    private val config: Config,
    private val idpClient: RestClient,
    private val syncStateRepository: SyncStateRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Fetches updates from the configured endpoint since the last persisted update timestamp.
     *
     *
     * @param type the type reference for the response body
     * @return a pair of the response body of type T and the latest timestamp reported by the API
     * @throws IllegalArgumentException if the response body or lastUpdated value is missing
     */
    fun <T> getUpdates(type: ParameterizedTypeReference<T>): Pair<T, Long> {
        val since =
            syncStateRepository
                .findById(config.orgid)
                .map { it.lastUpdated }
                .orElse(0L)
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
        return result to lastUpdated
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
        idpClient
            .get()
            .uri(uri)
            .retrieve()
            .body(type)
            .let { body ->
                logger.info("GET $uri")
                requireNotNull(body) { "No response body from $uri" }
            }
}
