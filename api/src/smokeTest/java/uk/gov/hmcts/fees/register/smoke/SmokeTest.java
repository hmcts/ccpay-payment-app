package uk.gov.hmcts.fees.register.smoke;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.functional.TestContextConfiguration;
import uk.gov.hmcts.payment.functional.idam.IdamService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests"})
@Slf4j
public class SmokeTest {
    @Value("${TEST_URL:http://localhost:8080}")
    private String testUrl;

    @Autowired
    private IdamService idamService;

    @Before
    public void setup() {
        RestAssured.baseURI = testUrl;
        log.info("Payments-Api base url is :{}", testUrl);
    }

    @Test
    public void testHealthCheck() {
        log.info("TEST - healthCheck() started");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body(
                "status", equalTo("UP"));
        assertFalse(testUrl.isEmpty(), "Sample Test for the template....");
        log.info("TEST - healthCheck() finished");
    }
}
