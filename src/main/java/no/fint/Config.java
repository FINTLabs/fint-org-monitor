package no.fint;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import no.fint.oauth.OAuthConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
@Import(OAuthConfig.class)
public class Config {

    @Bean("endpoint")
    public String endpoint(@Value("${fint.orgmonitor.endpoint}") String uri) {
        return uri;
    }

    @Bean("orgid")
    public String orgid(@Value("${fint.orgmonitor.orgid}") String orgid) {
        return orgid;
    }

    @Bean
    public Credentials credentials(
            @Value("${fint.orgmonitor.serviceaccount}") Path serviceAccount,
            @Value("${fint.orgmonitor.gmail.scopes:https://www.googleapis.com/auth/gmail.send}") String[] scopes,
            @Value("${fint.orgmonitor.gmail.delegate}") String delegate
    ) throws IOException {
        InputStream in = Files.newInputStream(serviceAccount);
        return ServiceAccountCredentials.fromStream(in).createScoped(scopes).createDelegated(delegate);
    }

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public JsonFactory jsonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

    @Bean
    public Gmail gmail(
            com.google.api.client.http.HttpTransport transport,
            com.google.api.client.json.JsonFactory jsonFactory,
            Credentials credentials,
            @Value("${fint.orgmonitor.application-name}") String applicationName
    ) {
        return new Gmail.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    @Bean("sender")
    public String sender(@Value("${fint.orgmonitor.sender}") String sender) { return sender; }

    @Bean("recipients")
    public List<String> recipients(
            @Value("${fint.orgmonitor.recipients}") String[] recipients) {
        return Arrays.asList(recipients);
    }

}
