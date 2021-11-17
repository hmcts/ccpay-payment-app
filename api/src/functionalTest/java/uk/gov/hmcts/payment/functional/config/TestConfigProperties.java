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

    @Value("${test.url}")
    public String baseTestUrl;

    @Value("${generated.user.email.pattern}")
    public String generatedUserEmailPattern;

    @Value("${generated.user.email.pattern.for.ref.data}")
    public String generatedUserEmailPatternForRefData;

    @Value("${test.user.password}")
    public String testUserPassword;

    @Value("${idam.api.url}")
    public String idamApiUrl;

    @Value("${ref.data.api.url}")
    public String refDataApiUrl;

    @Value("${s2s.url}")
    private String s2sBaseUrl;

    @Value("${s2s.service.name}")
    public String s2sServiceName;

    @Value("${s2s.service.secret}")
    public String s2sServiceSecret;

    @Value("${payments.account.existing.account.number}")
    public String existingAccountNumber;

    @Value("${payments.account.fake.account.number}")
    public String fakeAccountNumber;

    @Value("${mock.callback.url.endpoint}")
    public String mockCallBackUrl;

    @Value("${paybubble.s2s.service.secret}")
    public String payBubbleS2SSecret;

    @Value("${s2s.service.payment.app.name}")
    public String paymentAppS2SName;

    @Value("${s2s.service.payment.app.secret}")
    public String paymentAppS2SSecret;

    @Value("${idam.paybubble.client.id}")
    public String idamPayBubbleClientID;

    @Value("${idam.paybubble.client.secret}")
    public String idamPayBubbleClientSecret;

    @Value("${idam.rd.professional.client.id}")
    public String idamRefDataApiClientId;

    @Value("${idam.rd.professional.client.secret}")
    public String idamRefDataApiClientSecret;

    @Value("${service.request.cpo.update.service.s2s.secret}")
    public String serviceRequestCpoUpdateServices2sSecret;

    @Value("${service.request.cpo.update.service.s2s.topic.name}")
    public String serviceRequestCpoUpdateServices2sTopicName;

}
