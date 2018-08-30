package uk.gov.hmcts.payment.functional.tokens;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import static io.restassured.RestAssured.post;

@TestComponent
public class UserTokenFactory {

    private final String baseUrl;

    @Autowired
    public UserTokenFactory(@Value("${base-urls.idam}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String validTokenForUser(String userId, String role) {
        return "Bearer " + post(baseUrl + "/testing-support/lease?id={id}&role={role}", userId, role).body().asString();
    }
}
