package no.fint;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class MailingService {
    @Autowired
    private Gmail gmail;

    @Autowired
    @Qualifier("recipients")
    private List<String> recipients;

    @Autowired
    @Qualifier("sender")
    private String sender;

    public String send(String content) {
        try {
            log.info("Creating email from {} to {} ...", sender, recipients);
            MimeMessage mimeMessage = createEmail(sender, recipients, String.format("Org Monitor %TF %<TR", new Date()), content);
            Message message = sendMessage("me", mimeMessage);
            return message.getId();
        } catch (MessagingException | IOException e) {
            log.error("Unable to send message!", e);
            return null;
        }
    }


    private MimeMessage createEmail(
            String from,
            List<String> to,
            String subject,
            String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

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

    private Message createMessageWithEmail(
            MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private Message sendMessage(
            String userId,
            MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = gmail.users().messages().send(userId, message).execute();
        log.info("Message: {}", message.toPrettyString());
        return message;
    }

}
