package no.fint

import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import no.fint.mailing.MailingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.mail.MessagingException
import javax.mail.Transport
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailingServiceTest {
    private val config = Config(
        orgid = "fintlabs.no",
        sender = "sender@fintlabs.no",
        smtpUsername = "user",
        smtpPassword = "password",
        smtpServer = "localhost",
        smtpPort = "25",
        recipients = listOf("recipient@fintlabs.no"),
    )

    private val mailingService = MailingService(config)

    @BeforeEach
    fun setUp() {
        mockkStatic(Transport::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Transport::class)
    }

    @Test
    fun `send returns true when mail is sent successfully`() {
        every { Transport.send(any()) } just runs

        assertTrue(mailingService.send("<html>test</html>"))
    }

    @Test
    fun `send returns false when MessagingException is thrown`() {
        every { Transport.send(any()) } throws MessagingException("SMTP error")

        assertFalse(mailingService.send("<html>test</html>"))
    }
}
