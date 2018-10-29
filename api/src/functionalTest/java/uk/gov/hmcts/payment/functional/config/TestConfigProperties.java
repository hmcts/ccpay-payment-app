package uk.gov.hmcts.payment.functional.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class TestConfigProperties {

    @Autowired
    public Oauth2 oauth2;

    @Value("${base.test.url}")
    public String baseTestUrl;

    @Value("${generated.user.email.pattern}")
    public String generatedUserEmailPattern;

    @Value("${test.user.password}")
    public String testUserPassword;

    @Value("${idam.api.url}")
    public String idamApiUrl;
    @Value("${s2s.url}")
    private String s2sBaseUrl;

    @Value("${cmc.service.name:cmc}")
    public String cmcServiceName;

    @Value("${cmc.service.secret}")
    public String cmcSecret;

    @Value("payments.account.existing.account.number")
    public String existingAccountNumber;

    @Value("${payments.account.fake.account.number}")
    public String fakeAccountNumber;
}
