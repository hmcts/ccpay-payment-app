package uk.gov.hmcts.payment.functional;

import java.io.IOException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.payment.functional.dsl.PaymentTestDsl;
import uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto.CreatePaymentRequestDtoBuilder;
import uk.gov.hmcts.payment.api.v1.contract.PaymentOldDto;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto.createPaymentRequestDtoWith;

public class CreatePaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentTestDsl scenario;

    @Value("${probate.user.id}")
    private String probateUserId;

    @Value("${probate.service.name}")
    private String probateServiceName;

    @Value("${probate.service.secret}")
    private String probateSecret;

    private CreatePaymentRequestDtoBuilder validRequest = createPaymentRequestDtoWith()
            .amount(100)
            .description("Description")
            .reference("Reference")
            .returnUrl("https://return-url");

    @Test
    public void validCreatePaymentRequestShouldResultIn201() throws IOException, Exception {
        scenario.given().userId(probateUserId).serviceId(probateServiceName, probateSecret)
                .when().createPayment(probateUserId, validRequest)
                .then().created((paymentDto -> {
                    assertThat(paymentDto.getAmount()).isEqualTo(100);
                    assertThat(paymentDto.getState()).isEqualTo(new PaymentOldDto.StateDto("created", false));
                    assertThat(paymentDto.getDescription()).isEqualTo("Description");
                    assertThat(paymentDto.getReference()).isEqualTo("Reference");
                    assertThat(paymentDto.getLinks().getCancel()).isNotNull();
                    assertThat(paymentDto.getLinks().getNextUrl()).isNotNull();
                })
        );
    }

    @Test
    public void paymentWithoutAmountShouldNotBeCreated() throws IOException, Exception {
        scenario.given().userId(probateUserId).serviceId(probateServiceName, probateSecret)
                .when().createPayment(probateUserId, validRequest.amount(null))
                .then().validationError("amount: must not be null");
    }

    @Test
    public void paymentWithoutDescriptionShouldNotBeCreated() throws IOException, Exception {
        scenario.given().userId(probateUserId).serviceId(probateServiceName, probateSecret)
                .when().createPayment(probateUserId, validRequest.description(null))
                .then().validationError("description: must not be empty");
    }

    @Test
    public void paymentWithoutReferenceShouldNotBeCreated() throws IOException, Exception {
        scenario.given().userId(probateUserId).serviceId(probateServiceName, probateSecret)
                .when().createPayment(probateUserId, validRequest.reference(null))
                .then().validationError("reference: must not be empty");
    }

    @Test
    public void paymentWithoutReturnUrlShouldNotBeCreated() throws IOException, Exception {
        scenario.given().userId(probateUserId).serviceId(probateServiceName, probateSecret)
                .when().createPayment(probateUserId, validRequest.returnUrl(null))
                .then().validationError("returnUrl: must not be empty");
    }
}
