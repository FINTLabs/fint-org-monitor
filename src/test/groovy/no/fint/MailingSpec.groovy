package no.fint

import no.fint.mailing.MailingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
@SpringBootTest
@ActiveProfiles('test')
class MailingSpec extends Specification{

    @Autowired
    MailingService mailingService

    def 'Mailing service works properly'() {
        when:
        def result = mailingService.send('Hello world!')

        then:
        result
    }
}
