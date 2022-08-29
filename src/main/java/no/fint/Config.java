package no.fint;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.fint.oauth.OAuthConfig;
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

    private String smtpUsername;

    private String smtpPassword;

    private String sender;

    private List<String> recipients;

}
