package no.fint.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.novari.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.novari.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource

object ResourceConverter {
    private val objectMapper =
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    fun toOrganisasjonselement(resource: OrganisasjonselementResource): Organisasjonselement {
        val asString = objectMapper.writeValueAsString(resource)
        return objectMapper.readValue(asString, Organisasjonselement::class.java)
    }
}
