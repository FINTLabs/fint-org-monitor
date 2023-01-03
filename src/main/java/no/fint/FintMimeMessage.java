package no.fint;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class FintMimeMessage {

    private final MimeMessage mimeMessage;

    public FintMimeMessage(Config config, String content, String subject) throws MessagingException {
        mimeMessage = new MimeMessage(createSession(config));

        mimeMessage.setFrom(new InternetAddress(config.getSender()));

        for (String recipient : config.getRecipients()) { addRecipient(recipient); }
        mimeMessage.setSubject(subject);
        mimeMessage.setText(content, "UTF-8", "html");
    }

    private void addRecipient(String recipient) throws MessagingException {
        mimeMessage.addRecipient(Message.RecipientType.TO,new InternetAddress(recipient));
    }

    private Session createSession(Config config) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getSmtpServer());
        props.put("mail.smtp.port", config.getSmtpPort());

        return Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
                    }
                });
    }

    public MimeMessage getMail() {
        return mimeMessage;
    }
}
