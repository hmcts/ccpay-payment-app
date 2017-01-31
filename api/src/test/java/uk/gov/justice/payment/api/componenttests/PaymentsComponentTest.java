package uk.gov.justice.payment.api.componenttests;

import org.junit.Test;
import uk.gov.justice.payment.api.contract.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.contract.PaymentDto;
import uk.gov.justice.payment.api.contract.PaymentDto.LinksDto;
import uk.gov.justice.payment.api.contract.PaymentDto.StateDto;
import uk.gov.justice.payment.api.contract.RefundPaymentRequestDto;
import uk.gov.justice.payment.api.model.Payment;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.payment.api.componenttests.sugar.RestActions.SERVICE_ID;
import static uk.gov.justice.payment.api.contract.CreatePaymentRequestDto.createPaymentRequestDtoWith;
import static uk.gov.justice.payment.api.contract.PaymentDto.paymentDtoWith;
import static uk.gov.justice.payment.api.contract.RefundPaymentRequestDto.refundPaymentRequestDtoWith;
import static uk.gov.justice.payment.api.model.Payment.paymentWith;

public class PaymentsComponentTest extends ComponentTestBase {

    @Test
    public void searchPayments() throws Exception {
        db.create(validPaymentWith().paymentReference("Ref1").applicationReference("appRef1"));
        db.create(validPaymentWith().paymentReference("Ref2").applicationReference("appRef2"));

        restActions.get("/payments/?payment_reference=Ref1")
                .andExpect(status().isOk())
                .andExpect(bodyAs(PaymentDto.class).containsExactly(PaymentDto::getPaymentReference, "Ref1"));

        restActions.get("/payments/?payment_reference=Ref999")
                .andExpect(status().isOk())
                .andExpect(bodyAs(PaymentDto.class).containsExactly());
    }

    @Test
    public void createPaymentValidationRules() throws Exception {
        CreatePaymentRequestDto validRequest = createPaymentRequestDtoWith()
                .amount(100)
                .applicationReference("applicationReference")
                .description("description")
                .email("email@email.com")
                .paymentReference("paymentReference")
                .returnUrl("https://returnUrl")
                .build();

        tryCreateAndExpect(validRequest.withAmount(null), "amount: may not be null");
        tryCreateAndExpect(validRequest.withAmount(0), "amount: must be greater than or equal to 1");
        tryCreateAndExpect(validRequest.withApplicationReference(null), "applicationReference: may not be empty");
        tryCreateAndExpect(validRequest.withApplicationReference(""), "applicationReference: may not be empty");
        tryCreateAndExpect(validRequest.withDescription(null), "description: may not be empty");
        tryCreateAndExpect(validRequest.withDescription(""), "description: may not be empty");
        tryCreateAndExpect(validRequest.withEmail(null), "email: may not be empty");
        tryCreateAndExpect(validRequest.withEmail(""), "email: may not be empty");
        tryCreateAndExpect(validRequest.withEmail("invalid@"), "email: not a well-formed email address");
        tryCreateAndExpect(validRequest.withPaymentReference(null), "paymentReference: may not be empty");
        tryCreateAndExpect(validRequest.withPaymentReference(""), "paymentReference: may not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl(null), "returnUrl: may not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl(""), "returnUrl: may not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl("invalid"), "returnUrl: must be a valid URL");
        tryCreateAndExpect(validRequest.withReturnUrl("http://invalid"), "returnUrl: must be a valid URL");
    }

    private void tryCreateAndExpect(CreatePaymentRequestDto requestBody, String expectedContent) throws Exception {
        restActions.post("/payments/", requestBody)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(expectedContent));
    }

    @Test
    public void createPaymentWithDuplicateApplicationReference() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentsOf("gov-pay-responses/status-created.json"))
                        .withHeader("Content-Type", "application/json")
                ));

        CreatePaymentRequestDto requestBody = createPaymentRequestDtoWith()
                .amount(100)
                .applicationReference("applicationReference")
                .description("description")
                .email("email@email.com")
                .paymentReference("paymentReference")
                .returnUrl("https://returnUrl")
                .build();

        restActions.post("/payments/", requestBody).andExpect(status().isCreated());
        restActions.post("/payments/", requestBody).andExpect(status().isConflict());
    }

    @Test
    public void getExistingPaymentShouldReturn200AndBody() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/GOV_PAY_ID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentsOf("gov-pay-responses/status-created.json"))
                        .withHeader("Content-Type", "application/json")
                ));

        Payment payment = db.create(validPaymentWith().govPayId("GOV_PAY_ID").applicationReference("Application reference"));

        restActions.get("/payments/GOV_PAY_ID")
                .andExpect(status().isOk())
                .andExpect(body().isEqualTo(
                        paymentDtoWith()   // as defined in .json file
                                .state(new StateDto("created", false))
                                .paymentId("GOV_PAY_ID")
                                .amount(12000)
                                .description("New passport application")
                                .applicationReference("Application reference")
                                .paymentReference("Payment reference")
                                .dateCreated(payment.getDateCreated())
                                .links(new LinksDto(
                                        new PaymentDto.LinkDto("https://www.payments.service.gov.uk/secure/7f4adfaa-d834-4657-9c16-946863655bb2", "GET"),
                                        new PaymentDto.LinkDto("http://localhost/payments/GOV_PAY_ID/cancel", "POST")
                                ))
                                .build()
                ));

        assertThat(db.getLastPaymentHistoryEntry().getAction()).isEqualTo("retrieve");
    }

    @Test
    public void getUnknownPaymentShouldReturn404() throws Exception {
        restActions.get("/payments/UNKNOWN_GOV_PAY_ID")
                .andExpect(status().isNotFound());
    }

    @Test
    public void cancelExistingPaymentShouldReturn204() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/GOV_PAY_ID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentsOf("gov-pay-responses/status-created.json"))
                        .withHeader("Content-Type", "application/json")
                ));

        stubFor(post(urlPathMatching("/v1/payments/GOV_PAY_ID/cancel"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(validPaymentWith().govPayId("GOV_PAY_ID").amount(100));

        restActions.post("/payments/GOV_PAY_ID/cancel")
                .andExpect(status().is(204));

        assertThat(db.getLastPaymentHistoryEntry().getAction()).isEqualTo("cancel");
    }

    @Test
    public void cancelPaymentFailureShouldReturn400() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments/GOV_PAY_ID/cancel"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{ \"code\": \"P0501\", \"description\": \"Cancellation of payment failed\" }")
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(validPaymentWith().govPayId("GOV_PAY_ID"));

        restActions.post("/payments/GOV_PAY_ID/cancel")
                .andExpect(status().is(400));
    }

    @Test
    public void refundPaymentValidationRules() throws Exception {
        RefundPaymentRequestDto validRequest = refundPaymentRequestDtoWith()
                .amount(100)
                .refundAmountAvailable(200)
                .build();

        tryRefundAndExpect(validRequest.withAmount(null), "amount: may not be null");
        tryRefundAndExpect(validRequest.withAmount(0), "amount: must be greater than or equal to 1");
        tryRefundAndExpect(validRequest.withRefundAmountAvailable(null), "refundAmountAvailable: may not be null");
        tryRefundAndExpect(validRequest.withRefundAmountAvailable(0), "refundAmountAvailable: must be greater than or equal to 1");
    }

    private void tryRefundAndExpect(RefundPaymentRequestDto requestBody, String expectedContent) throws Exception {
        restActions.post("/payments/1/refunds", requestBody)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(expectedContent));
    }

    @Test
    public void refundPaymentShouldReturn201OnSuccess() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/GOV_PAY_ID"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentsOf("gov-pay-responses/status-created.json"))
                        .withHeader("Content-Type", "application/json")
                ));

        stubFor(post(urlPathMatching("/v1/payments/GOV_PAY_ID/refunds"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(validPaymentWith()
                .applicationReference("REFUND_APPLICATION_REFERENCE")
                .govPayId("GOV_PAY_ID"));

        restActions
                .post("/payments/REFUND_APPLICATION_REFERENCE/refunds", refundPaymentRequestDtoWith().amount(100).refundAmountAvailable(100).build())
                .andExpect(status().is(201));

        assertThat(db.getLastPaymentHistoryEntry().getAction()).isEqualTo("refund");
    }

    @Test
    public void refundPaymentShouldReturn412IfAmountMismatch() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments/GOV_PAY_ID/refunds"))
                .willReturn(aResponse()
                        .withStatus(412)
                        .withBody("{ \"code\": \"P0604\", \"description\": \"Refund amount available mismatch\" }")
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(validPaymentWith()
                .govPayId("GOV_PAY_ID")
                .applicationReference("REFUND_APPLICATION_REFERENCE")
                .amount(100));

        restActions
                .post("/payments/REFUND_APPLICATION_REFERENCE/refunds", refundPaymentRequestDtoWith().amount(100).refundAmountAvailable(100).build())
                .andExpect(status().is(412));
    }

    private Payment.PaymentBuilder validPaymentWith() {
        return paymentWith()
                .serviceId(SERVICE_ID)
                .amount(100)
                .status("status")
                .govPayId("paymentId")
                .description("description")
                .email("email@email.com")
                .applicationReference("applicationReference")
                .paymentReference("paymentReference")
                .returnUrl("returnUrl")
                .selfUrl(resolvePlaceholders("${gov.pay.url}/GOV_PAY_ID"))
                .cancelUrl(resolvePlaceholders("${gov.pay.url}/GOV_PAY_ID/cancel"))
                .refundsUrl(resolvePlaceholders("${gov.pay.url}/GOV_PAY_ID/refunds"))
                .finished(true);
    }

}