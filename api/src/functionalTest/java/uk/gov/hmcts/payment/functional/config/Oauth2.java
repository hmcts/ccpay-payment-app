package uk.gov.hmcts.payment.functional.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Oauth2 {

    private final String clientId;
    private final String redirectUrl;
    private final String clientSecret;

    @Autowired
    public Oauth2(
        @Value("${oauth2.redirect.uri}") String redirectUrl,
        @Value("${oauth2.client.id}") String clientId,
        @Value("${oauth2.client.secret}") String clientSecret) {
        this.clientId = clientId;
        this.redirectUrl = redirectUrl + "/receiver";
        this.clientSecret = clientSecret;
    }
}
