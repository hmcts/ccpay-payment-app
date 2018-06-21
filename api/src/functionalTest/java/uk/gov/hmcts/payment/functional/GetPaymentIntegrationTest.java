package uk.gov.hmcts.payment.functional;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.functional.dsl.PaymentTestDsl;
import uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto;
import uk.gov.hmcts.payment.api.v1.contract.PaymentOldDto;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto.createPaymentRequestDtoWith;

public class GetPaymentIntegrationTest extends IntegrationTestBase {

    private CreatePaymentRequestDto.CreatePaymentRequestDtoBuilder validRequest = createPaymentRequestDtoWith()
            .amount(100)
            .description("Description")
            .reference("Reference")
            .returnUrl("https://return-url");

    @Autowired
    private PaymentTestDsl scenario;

    @Test
    public void validGetPaymentRequestShouldResultIn200() throws IOException, Exception {
        AtomicReference<PaymentOldDto> paymentHolder = new AtomicReference<>();

        scenario.given().userId("1").serviceId("reference")
                .when()
                .createPayment("1", validRequest, paymentHolder)
                .getPayment("1", paymentHolder.get().getId())
                .then().get((paymentDto -> {
                    assertThat(paymentDto.getAmount()).isEqualTo(100);
                    assertThat(paymentDto.getState()).isEqualTo(new PaymentOldDto.StateDto("created", false));
                    assertThat(paymentDto.getDescription()).isEqualTo("Description");
                    assertThat(paymentDto.getReference()).isEqualTo("Reference");
                    assertThat(paymentDto.getLinks().getCancel().getHref()).endsWith(paymentHolder.get().getId() + "/cancel");
                    assertThat(paymentDto.getLinks().getNextUrl()).isNotNull();
                })
        );
    }

    @Test
    public void createAndCancelApproach() throws IOException, Exception {
        AtomicReference<PaymentOldDto> paymentHolder = new AtomicReference<>();

        scenario.given().userId("1").serviceId("reference")
                .when()
                .createPayment("1", validRequest, paymentHolder)
                .cancelPayment("1", paymentHolder.get().getId())
                .then().cancelled();
    }

    @Test
    public void getPaymentWithoutPaymentIdRequestShouldResultIn500() throws IOException { // TODO: fix, should be 404
        scenario.given().userId("1").serviceId("reference")
                .when().getPayment("1", "")
                .then().validationErrorfor500("");
    }

    @Test
    public void getPaymentWithoutUserIdShouldResultIn404() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().getPayment("", "9")
                .then().notFound();
    }

    @Test
    public void getPaymentWithoutUserIdAndPaymentIdRequestShouldResultIn400() throws IOException {
        scenario.given().userId("1").serviceId("reference")
                .when().getPayment("", "")
                .then().notFound();
    }

}
