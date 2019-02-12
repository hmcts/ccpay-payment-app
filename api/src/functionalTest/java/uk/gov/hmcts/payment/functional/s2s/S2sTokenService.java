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
        return "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwcm9iYXRlX2Zyb250ZW5kIiwiZXhwIjoxNTQ5NjUwMDY2fQ.HKMoXQ2ko_2cRBRPEVrTjtxAIbCzn7Ch_txcda9wABJuRsCGnFpA_Ci57Gy3adZpzxCmwu_ZZU70q0OPsA291w";
    }
}
