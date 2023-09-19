package no.fint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Slf4j
@Component
public class RestUtil {

    private final RestTemplate restTemplate;
    private final ConcurrentMap<String, Long> lastUpdatedMap;

    public RestUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        lastUpdatedMap = new ConcurrentSkipListMap<>();
    }

    public <T> T getUpdates(ParameterizedTypeReference<T> type, String uri) {
        long lastUpdated = Long.parseLong(
                get(new ParameterizedTypeReference<Map<String,String>>() {
                }, UriComponentsBuilder
                        .fromUriString(uri)
                        .pathSegment("last-updated")
                        .build()
                        .toUriString())
                        .get("lastUpdated")
        );
        long since = lastUpdatedMap.getOrDefault(uri, 0L);
        log.info("Fetching {} since {}, last updated {} ...", uri, since, lastUpdated);
        T result = get(type, UriComponentsBuilder.fromUriString(uri).queryParam("sinceTimeStamp", since).build().toUriString());
        lastUpdatedMap.put(uri, lastUpdated);
        return result;
    }

    public <T> T get(ParameterizedTypeReference<T> type, String uri) {
        log.info("GET {}", uri);
        ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, null, type);
        log.info("Response: {}", response.getStatusCode());
        return response.getBody();
    }

}
