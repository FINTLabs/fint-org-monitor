package no.fint


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Requires
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@ActiveProfiles('test')
@Requires({ Files.exists(Paths.get('serviceaccount.json')) })
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
