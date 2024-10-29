package uk.gov.hmcts.payment.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@EnableOAuth2Client
@Configuration
public class LiberataOAuth2RestOperationsConfiguration {

    @Value("${liberata.oauth2.token.url}")
    private String tokenUrl;

    @Value("${liberata.oauth2.client.id}")
    private String clientId;

    @Value("${liberata.oauth2.client.secret}")
    private String clientSecret;

    @Value("${liberata.oauth2.username}")
    private String username;

    @Value("${liberata.oauth2.password}")
    private String password;

    @Bean
    protected OAuth2ProtectedResourceDetails resourceDetails() {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(tokenUrl);
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource.setGrantType("password");
        resource.setUsername(username);
        resource.setPassword(password);

        return resource;
    }

    @Bean
    public OAuth2RestOperations restTemplate() {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();

        return new OAuth2RestTemplate(resourceDetails(), new DefaultOAuth2ClientContext(atr));
    }
}
