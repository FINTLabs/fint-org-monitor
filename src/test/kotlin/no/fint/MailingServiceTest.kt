package no.fint

import no.fint.mailing.MailingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class MailingServiceTest {
    companion object {
        @Container
        @ServiceConnection // Spring Boot will auto-wire spring.datasource.* from this container
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
    }

    @Autowired
    lateinit var mailingService: MailingService
}
