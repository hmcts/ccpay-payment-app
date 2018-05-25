package uk.gov.hmcts.payment.functional.tokens;


import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import static io.restassured.RestAssured.post;

@TestComponent
public class ServiceTokenFactory {

    private final String baseUrl;

    @Autowired
    public ServiceTokenFactory(@Value("${base-urls.service-auth-provider}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String validTokenForService(String microservice) {
        return "Bearer " + RestAssured.given().relaxedHTTPSValidation().post(baseUrl + "/testing-support/lease?microservice={microservice}", microservice).body().asString();
    }
}
