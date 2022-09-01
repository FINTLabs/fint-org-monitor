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
            String subject = String.format("Org Monitor %TF %<TR", new Date());
            MimeMessage mimeMessage = new FintMimeMessage(config, content, subject).getMail();
            Transport.send(mimeMessage);
            return true;
        } catch (MessagingException e) {
            log.error("Unable to send message!", e);
            return false;
        }
    }
}
