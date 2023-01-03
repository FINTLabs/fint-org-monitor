package no.fint;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.fint.oauth.OAuthConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "fint.orgmonitor")
@Component
@Data
@Slf4j
@Import(OAuthConfig.class)
public class Config {

    private String endpoint;

    private String orgid;

    private String sender;

    @Value("smtp-username")
    private String smtpUsername;

    @Value("smtp-password")
    private String smtpPassword;

    @Value("smtp-server")
    private String smtpServer;

    @Value("smtp-port")
    private String smtpPort;

    private List<String> recipients;

}
