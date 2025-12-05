package no.fint.mailing

import no.fint.Config
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class FintMimeMessage(
    config: Config,
    content: String,
    subject: String,
) {
    val mail: MimeMessage =
        MimeMessage(createSession(config)).apply {
            setFrom(InternetAddress(config.sender))
            config.recipients.forEach { recipient ->
                addRecipient(Message.RecipientType.TO, InternetAddress(recipient))
            }
            setSubject(subject)
            setText(content, "UTF-8", "html")
        }

    private fun createSession(config: Config): Session {
        val props =
            Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", config.smtpServer)
                put("mail.smtp.port", config.smtpPort)
            }

        return Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(config.smtpUsername, config.smtpPassword)
            },
        )
    }
}
