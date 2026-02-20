package no.fint.mailing

import no.fint.Config
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.mail.MessagingException
import javax.mail.Transport
import javax.mail.internet.MimeMessage

@Service
class MailingService(
    private val config: Config,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun send(content: String): Boolean {
        logger.info("Sending email from ${config.sender} to ${config.recipients}")
        try {
            Transport.send(createMimeMessage(content))
        } catch (e: MessagingException) {
            logSendEmailError(e)
            return false
        }
        return true
    }

    private fun createMimeMessage(content: String): MimeMessage = FintMimeMessage(config, content, generateSubject()).mail

    private fun generateSubject() =
        java.time.LocalDateTime.now().let { now ->
            "Org Monitor for ${config.orgid} ${now.toLocalDate()} ${now.toLocalTime().withNano(0) }"
        }

    private fun logSendEmailError(e: MessagingException) {
        logger.error(
            "Unable to send message! smtpUsername: ${config.smtpUsername}, smtpHost: ${config.smtpServer}, smtpPost: ${config.smtpPort}, Error: $e",
        )
    }
}
