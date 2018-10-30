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
        return "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwcm9iYXRlX2Zyb250ZW5kIiwiZXhwIjoxNTQwODQxMDI3fQ.jmtkT2i6cY-rGcZ3Dgwe31q9F2yiu4PhH9zJD5SDc81svHG9BkeG_uqoKajPXZObrogI65QVd-Urvh5iMmxF9A";
//        try {
//            return s2sApi.serviceToken(microservice, otp);
//        } catch (Exception ex) {
//            throw ex;
//        }
    }
}
