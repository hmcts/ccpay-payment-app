package uk.gov.hmcts.payment.functional.s2s;


import feign.Feign;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;

@Component
public class S2sTokenService {

    private final TestConfigProperties testProps;
    private final OneTimePasswordFactory oneTimePasswordFactory;
    private final S2sApi s2sApi;

    @Autowired
    public S2sTokenService(OneTimePasswordFactory oneTimePasswordFactory, TestConfigProperties testProps) {
        this.oneTimePasswordFactory = oneTimePasswordFactory;
        this.testProps = testProps;
        s2sApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .target(S2sApi.class, testProps.getS2sBaseUrl());
    }

    public String getS2sToken(String microservice, String secret) {
        String otp = oneTimePasswordFactory.validOneTimePassword(secret);
        System.out.println("otp >> "+otp);
        System.out.println("microservice >> "+microservice);
        System.out.println("secret >> "+secret);
        System.out.println("testProps.getS2sBaseUrl() >> "+testProps.getS2sBaseUrl());
        String s2sToken = s2sApi.serviceToken(microservice, otp);
        System.out.println("s2sToken >> "+s2sToken);
        return s2sToken;
    }
}
