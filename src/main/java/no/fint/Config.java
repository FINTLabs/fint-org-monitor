package no.fint;

import lombok.extern.slf4j.Slf4j;
import no.fint.oauth.OAuthConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Slf4j
@Import(OAuthConfig.class)
public class Config {

    @Bean("endpoint")
    @Value("${fint.orgmonitor.endpoint}")
    public String endpoint(String uri) {
        return uri;
    }

    @Bean("orgid")
    @Value("${fint.orgmonitor.orgid}")
    public String orgid(String orgid) { return orgid; }

}
