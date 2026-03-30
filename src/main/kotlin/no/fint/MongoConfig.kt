package no.fint

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.model.administrasjon.organisasjon.Organisasjonselement
import org.bson.Document
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
class MongoConfig {
    private val objectMapper =
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions(
            listOf(
                OrganisasjonselementWriteConverter(objectMapper),
                OrganisasjonselementReadConverter(objectMapper),
            ),
        )
}

class OrganisasjonselementWriteConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Organisasjonselement, Document> {
    override fun convert(source: Organisasjonselement): Document = Document.parse(objectMapper.writeValueAsString(source))
}

class OrganisasjonselementReadConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Document, Organisasjonselement> {
    override fun convert(source: Document): Organisasjonselement = objectMapper.readValue(source.toJson(), Organisasjonselement::class.java)
}
