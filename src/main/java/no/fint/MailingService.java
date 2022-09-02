package no.fint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class MailingService {

    private final Config config;

    public MailingService(Config config) {
        this.config = config;
    }

    public boolean send(String content) {
        try {
            log.info("Creating email from {} to {} ...", config.getSmtpUsername(), config.getRecipients());
            MimeMessage mimeMessage = createEmail(
                    config.getSmtpUsername(),
                    config.getRecipients(),
                    String.format("Org Monitor %TF %<TR", new Date()),
                    content);
            Transport.send(mimeMessage);
            return true;
        } catch (MessagingException e) {
            log.error("Unable to send message! \nsmtpUsername: {} smtpHost: {} smtpPort: {}",
                    config.getSmtpUsername(), config.getSmtpHost(), config.getSmtpPort(), e);
            return false;
        }
    }

    private MimeMessage createEmail(
            String from,
            List<String> to,
            String subject,
            String bodyText)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
                    }
                });

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        for (String recipient : to) {
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(recipient));
        }
        email.setSubject(subject);
        email.setText(bodyText, "UTF-8", "html");
        return email;
    }
}
