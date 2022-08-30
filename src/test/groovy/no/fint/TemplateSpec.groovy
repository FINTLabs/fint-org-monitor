package no.fint

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.fint.model.felles.kompleksedatatyper.Identifikator
import org.jooq.lambda.tuple.Tuple2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Requires
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@ActiveProfiles('test')
class TemplateSpec extends Specification{

    @Autowired
    TemplateService templateService

    def 'Template renders properly'() {
        given:
        def added = [
                new OrganisationDocument(
                        data: new Organisasjonselement(
                                navn: 'En liten test',
                                kortnavn: 'test',
                                organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                organisasjonsId: new Identifikator(identifikatorverdi: '2')
                        )
                )
        ]
        def updated = [
                Tuple2.tuple(
                        new OrganisationDocument(
                                data: new Organisasjonselement(
                                        navn: 'Gammelt navn',
                                        kortnavn: 'gammel',
                                        organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                        organisasjonsId: new Identifikator(identifikatorverdi: '2')
                                )
                        ),
                        new OrganisationDocument(
                                data: new Organisasjonselement(
                                        navn: 'Nytt navn',
                                        kortnavn: 'ny',
                                        organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                        organisasjonsId: new Identifikator(identifikatorverdi: '2')
                                )
                        )
                )
        ]

        when:
        String result = templateService.render(added, updated)
        println(result)

        then:
        result.contains('er nye:')
        result.contains('er endret:')
        result.contains('En liten test')
        result.contains('Gammelt navn')
        result.contains('Nytt navn')
    }

    def 'Template renders with empty inputs'() {
        when:
        String result = templateService.render([], [])
        println(result)

        then:
        !result.contains('er nye:')
        !result.contains('er endret:')
    }

    def 'Renders example JSON'() {
        given:
        def json = new ObjectMapper()
        def org = json.readValue(getClass().getResourceAsStream('/testorg.json'), OrganisationDocument)

        when:
        println(org)
        def result = templateService.render([org], [])
        println(result)

        then:
        result.contains('VGKALN')
        result.contains('974544520')
    }
}
