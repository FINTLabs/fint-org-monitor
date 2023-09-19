package no.fint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Date;

@Service
@Slf4j
public class MailingService {

    private final Config config;

    public MailingService(Config config) {
        this.config = config;
    }

    public boolean send(String content) {
        try {
            log.info("Creating email from {} to {} ...", config.getSender(), config.getRecipients());
            Transport.send(createMimeMessage(content));
            return true;
        } catch (MessagingException e) {
            logSendEmailError(e);
            return false;
        }
    }

    private MimeMessage createMimeMessage(String content) throws MessagingException {
        String subject = generateSubject();
        return new FintMimeMessage(config, content, subject).getMail();
    }

    private String generateSubject() {
        return String.format("Org Monitor %TF %<TR", new Date());
    }

    private void logSendEmailError(MessagingException e) {
        log.error("Unable to send message! smtpUsername: {} smtpHost: {} smtpPort: {}",
                config.getSmtpUsername(), config.getSmtpServer(), config.getSmtpPort(), e);
    }
}
