package uk.gov.hmcts.payment.functional.tokens;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import static io.restassured.RestAssured.post;

@TestComponent
public class ServiceTokenFactory {

    private final String baseUrl;

    @Autowired
    private OneTimePasswordFactory otpFactory;

    @Autowired
    public ServiceTokenFactory(@Value("${base-urls.service-auth-provider}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String validTokenForService(String microservice) {
        String otp = otpFactory.validOneTimePassword("AAAAAAAAAAAAAAAA");
        return "Bearer " + post(baseUrl + "/lease?oneTimePassword={otp}&microservice={microservice}", otp, microservice).body().asString();
    }
}
