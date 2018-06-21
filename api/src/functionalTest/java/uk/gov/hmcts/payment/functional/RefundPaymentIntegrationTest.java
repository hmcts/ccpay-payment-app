package uk.gov.hmcts.payment.functional;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.functional.dsl.PaymentTestDsl;
import uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto;
import uk.gov.hmcts.payment.api.v1.contract.PaymentOldDto;
import uk.gov.hmcts.payment.api.v1.contract.RefundPaymentRequestDto;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto.createPaymentRequestDtoWith;
import static uk.gov.hmcts.payment.api.v1.contract.RefundPaymentRequestDto.refundPaymentRequestDtoWith;

public class RefundPaymentIntegrationTest extends IntegrationTestBase {

    private CreatePaymentRequestDto.CreatePaymentRequestDtoBuilder validRequest = createPaymentRequestDtoWith()
        .amount(100)
        .description("Description")
        .reference("Reference")
        .returnUrl("https://return-url");

    private RefundPaymentRequestDto.RefundPaymentRequestDtoBuilder refundValidRequest = refundPaymentRequestDtoWith()
        .amount(5)
        .refundAmountAvailable(100);

    private RefundPaymentRequestDto.RefundPaymentRequestDtoBuilder refundAmountAvailableInvalidRequest = refundPaymentRequestDtoWith()
        .amount(5)
        .refundAmountAvailable(1000);

    @Autowired
    private PaymentTestDsl scenario;

    @Test
    public void createAndRefundPayment() throws IOException, Exception {
        AtomicReference<PaymentOldDto> paymentHolder = new AtomicReference<>();
        scenario.given().userId("1").serviceId("reference")
            .when()
            .createPayment("1", validRequest, paymentHolder)
            .refundPayment("1", refundValidRequest, paymentHolder.get().getId())
            .then().refundPayment();
    }

    @Test
    public void createAndRefundAvailableAmountInvalid() throws IOException, Exception {
        AtomicReference<PaymentOldDto> paymentHolder = new AtomicReference<>();
        scenario.given().userId("1").serviceId("reference")
            .when()
            .createPayment("1", validRequest, paymentHolder)
            .refundPayment("1", refundAmountAvailableInvalidRequest, paymentHolder.get().getId())
            .then().refundAvailableAmountInvalid412();
    }

}
