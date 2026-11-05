package uk.gov.hmcts.payment.smoke;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.functional.TestContextConfiguration;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@Slf4j
public class SmokeTest {
    @Value("${TEST_URL:http://localhost:8080}")
    private String testUrl;

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private PaymentTestService paymentTestService;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Before
    public void setup() {
        RestAssured.baseURI = testUrl;
        log.info("Payment Api base url is :{}", testUrl);
    }

    @Test
    public void testHealthCheck() {
        SerenityRest.given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body(
                "status", equalTo("UP"));
        assertFalse(testUrl.isEmpty(), "Sample Test for the template....");
    }

    @Test
    public void testLiberataPBAAccountCheck() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String userToken = idamService.authenticateUser(testProps.getProbateSolicitorUser(), testProps.getProbateSolicitorPassword());
        String serviceToken = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

        String accountNumber = testProps.fakeAccountNumber;

        // Get pba account by PBA number
        paymentTestService.getPbaAccountDetails(userToken, serviceToken, accountNumber)
            .then()
            .statusCode(NOT_FOUND.value())
            .body(equalTo("Account not found"));
    }


}
