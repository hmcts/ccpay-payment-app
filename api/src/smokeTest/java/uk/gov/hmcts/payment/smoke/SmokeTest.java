package uk.gov.hmcts.payment.smoke;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.smoke.config.TestConfigProperties;
import uk.gov.hmcts.payment.smoke.idam.IdamService;
import uk.gov.hmcts.payment.smoke.s2s.S2sTokenService;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class SmokeTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

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
    }

    @Test
    public void testLiberataPBAAccountCheck() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String userToken = idamService.authenticateUser(testProps.getProbateSolicitorUser(), testProps.getProbateSolicitorPassword());
        String serviceToken = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

        String accountNumber = testProps.probatePbaAccountNumber;

        // Get pba account by PBA number
        SerenityRest.given()
            .header("Authorization", userToken)
            .header("ServiceAuthorization", serviceToken)
            .header("Content-Type", "application/json")
            .when()
            .get("/accounts/{accountNumber}", accountNumber)
            .then()
            .statusCode(OK.value())
            .body("status", equalTo("Active"))
            .body("account_number", equalTo(accountNumber));
    }

}
