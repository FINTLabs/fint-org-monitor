package no.fint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;

import java.io.IOException;

public class ResourceConverter {
    public static Organisasjonselement toOrganisasjonselement(OrganisasjonselementResource resource) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String asString = objectMapper.writeValueAsString(resource);
        return objectMapper.readValue(asString, Organisasjonselement.class);
    }
}
