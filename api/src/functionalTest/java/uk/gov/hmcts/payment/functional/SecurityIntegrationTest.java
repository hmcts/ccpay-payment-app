package uk.gov.hmcts.payment.functional;

import java.io.IOException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.functional.dsl.PaymentTestDsl;

public class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentTestDsl scenario;

    @Test
    public void noUserAndServiceTokenShouldResultIn403() throws IOException {
        scenario.given()
                .when().getPayment("1", "999999")
                .then().forbidden();
    }

    @Test
    public void noUserTokenShouldResultIn403() throws IOException {
        scenario.given().serviceId("reference")
                .when().getPayment("1", "999999")
                .then().forbidden();
    }

    @Test
    public void noServiceTokenShouldResultIn403() throws IOException {
        scenario.given().userId("1")
                .when().getPayment("1", "999999")
                .then().forbidden();
    }

    @Test
    public void validUserAndServiceTokenShouldNotResultIn403() throws IOException {
        scenario.given().serviceId("reference").userId("1")
                .when().getPayment("1", "999999")
                .then().notFound();
    }

    @Test
    public void callFromUnknownServiceShouldResultIn403() throws IOException {
        scenario.given().serviceId("unknown-service").userId("1")
                .when().getPayment("1", "999999")
                .then().forbidden();
    }

    @Test
    public void callToOtherUsersResourceShouldResultIn403() throws IOException {
        scenario.given().serviceId("reference").userId("1")
                .when().getPayment("2", "999999")
                .then().forbidden();
    }
}
