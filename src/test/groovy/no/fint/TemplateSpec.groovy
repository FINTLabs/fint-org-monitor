package no.fint

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.model.administrasjon.organisasjon.Organisasjonselement
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.organization.SimpleOrganizationInfo
import no.fint.utils.TemplateService
import org.jooq.lambda.tuple.Tuple2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import no.fint.organization.OrganizationDocument
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
class TemplateSpec extends Specification{

    @Autowired
    TemplateService templateService

    def 'Template renders properly'() {
        given:
        def added = [
                new OrganizationDocument(
                        data: new Organisasjonselement(
                                navn: 'En liten test',
                                kortnavn: 'test',
                                organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                organisasjonsId: new Identifikator(identifikatorverdi: '2'),
                                gyldighetsperiode: new Periode(){{
                                    setStart(new Date())
                                    setSlutt(new Date())
                                }}
                        )
                )
        ]
        def updated = [
                Tuple2.tuple(
                        new OrganizationDocument(
                                data: new Organisasjonselement(
                                        navn: 'Gammelt navn',
                                        kortnavn: 'gammel',
                                        organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                        organisasjonsId: new Identifikator(identifikatorverdi: '2'),
                                        gyldighetsperiode: new Periode(){{
                                            setStart(new Date())
                                            setSlutt(new Date())
                                        }}
                                )
                        ),
                        new OrganizationDocument(
                                data: new Organisasjonselement(
                                        navn: 'Nytt navn',
                                        kortnavn: 'ny',
                                        organisasjonsKode: new Identifikator(identifikatorverdi: '123'),
                                        organisasjonsId: new Identifikator(identifikatorverdi: '2'),
                                        gyldighetsperiode: new Periode(){{
                                            setStart(new Date())
                                            setSlutt(new Date())
                                        }}
                                )
                        )
                )
        ]
        def parentInfo = [
                new SimpleOrganizationInfo("1", "Østfold fylkeskommune")
                ]

        when:
        String result = templateService.render(added, updated, parentInfo)
        println(result)

        then:
        result.contains('er nye:')
        result.contains('er endret:')
        result.contains('En liten test')
        result.contains('Gammelt navn')
        result.contains('Nytt navn')
        result.contains('Overordnede organisasjonsenheter:')
    }

    def 'Template renders with empty inputs'() {
        when:
        String result = templateService.render([], [], [])
        println(result)

        then:
        !result.contains('er nye:')
        !result.contains('er endret:')
        !result.contains('Overordnede organisasjonsenheter:')
    }

    def 'Renders example JSON'() {
        given:
        def json = new ObjectMapper()
        def org = json.readValue(getClass().getResourceAsStream('/testorg.json'), OrganizationDocument)

        when:
        println(org)
        def result = templateService.render([org], [], [])
        println(result)

        then:
        result.contains('VGKALN')
        result.contains('974544520')
    }
}
