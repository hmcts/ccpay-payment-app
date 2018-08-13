/*package uk.gov.hmcts.payment.functional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.functional.dsl.PaymentTestDsl;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest.CardPaymentRequestBuilder;
import uk.gov.hmcts.payment.api.contract.PaymentDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.contract.CardPaymentRequest.createCardPaymentRequestDtoWith;

public class CreatePaymentIntegration2Test extends IntegrationTestBase {

    @Autowired
    private PaymentTestDsl scenario;

    private CardPaymentRequestBuilder validRequest = createCardPaymentRequestDtoWith()
            .amount(145.50)
            .description("Description")
            .reference("Reference")
            .returnUrl("https://return-url");

    @Test
    public void validCreatePaymentRequestShouldResultIn201() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().createPayment("1", validRequest)
                .then().created((PaymentOldDto -> {
                    assertThat(PaymentOldDto.getAmount()).isEqualTo(100);
                    assertThat(PaymentOldDto.getState()).isEqualTo(new PaymentOldDto.StateDto("created", false));
                    assertThat(PaymentOldDto.getDescription()).isEqualTo("Description");
                    assertThat(PaymentOldDto.getReference()).isEqualTo("Reference");
                    assertThat(PaymentOldDto.getLinks().getCancel()).isNotNull();
                    assertThat(PaymentOldDto.getLinks().getNextUrl()).isNotNull();
                })
        );
    }

    @Test
    public void paymentWithoutAmountShouldNotBeCreated() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().createPayment("1", validRequest.amount(null))
                .then().validationError("amount: may not be null");
    }

    @Test
    public void paymentWithoutDescriptionShouldNotBeCreated() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().createPayment("1", validRequest.description(null))
                .then().validationError("description: may not be empty");
    }

    @Test
    public void paymentWithoutReferenceShouldNotBeCreated() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().createPayment("1", validRequest.reference(null))
                .then().validationError("reference: may not be empty");
    }

    @Test
    public void paymentWithoutReturnUrlShouldNotBeCreated() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().createPayment("1", validRequest.returnUrl(null))
                .then().validationError("returnUrl: may not be empty");
    }
}
*/
