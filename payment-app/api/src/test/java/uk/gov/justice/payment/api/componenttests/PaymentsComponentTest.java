package uk.gov.justice.payment.api.componenttests;

import org.junit.Test;
import uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto.LinksDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto.StateDto;
import uk.gov.justice.payment.api.controllers.dto.RefundPaymentRequestDto;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.payment.api.componenttests.sugar.RestActions.SERVICE_ID;
import static uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto.createPaymentRequestDtoWith;
import static uk.gov.justice.payment.api.controllers.dto.PaymentDto.paymentDtoWith;
import static uk.gov.justice.payment.api.controllers.dto.RefundPaymentRequestDto.refundPaymentRequestDtoWith;
import static uk.gov.justice.payment.api.model.PaymentDetails.paymentDetailsWith;

public class PaymentsComponentTest extends ComponentTestBase {

    @Test
    public void searchPayments() throws Exception {
        db.create(paymentDetailsWith().paymentReference("Ref1").serviceId(SERVICE_ID));
        db.create(paymentDetailsWith().paymentReference("Ref2").serviceId(SERVICE_ID));

        restActions.get("/payments/?payment_reference=Ref1")
                .andExpect(status().isOk())
                .andExpect(bodyAs(PaymentDto.class).containsExactly(
                        paymentDtoWith().paymentReference("Ref1").state(new StateDto()).links(new LinksDto()).build()
                ));

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
    public void getExistingPaymentShouldReturn200AndBody() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(contentsOf("viewPaymentResponse.json"))
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(paymentDetailsWith()
                .paymentId("123")
                .amount(12000)
                .description("Description")
                .applicationReference("Application reference")
                .paymentReference("Payment reference")
                .nextUrl("https://www.payments.service.gov.uk/secure/7f4adfaa-d834-4657-9c16-946863655bb2")
                .cancelUrl("https://www.payments.service.gov.uk/secure/7f4adfaa-d834-4657-9c16-946863655bb2/cancel")
                .createdDate("Created date")
        );

        restActions.get("/payments/123")
                .andExpect(status().isOk())
                .andExpect(body().isEqualTo(
                        paymentDtoWith()   // as defined in .json file
                                .state(new StateDto("created", false))
                                .paymentId("123")
                                .amount(12000)
                                .description("Description")
                                .applicationReference("Application reference")
                                .paymentReference("Payment reference")
                                .createdDate("Created date")
                                .links(new LinksDto(
                                        new PaymentDto.LinkDto("https://www.payments.service.gov.uk/secure/7f4adfaa-d834-4657-9c16-946863655bb2", "GET"),
                                        new PaymentDto.LinkDto("http://localhost/payments/123/cancel", "POST")
                                ))
                                .build()
                ));
    }

    @Test
    public void getUnknownPaymentShouldReturn404() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/-1"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{ \"code\": \"P0200\" }")
                        .withHeader("Content-Type", "application/json")
                ));

        restActions.get("/payments/-1")
                .andExpect(status().isNotFound());
    }

    @Test
    public void cancelExistingPaymentShouldReturn204() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments/1/cancel"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "application/json")
                ));

        restActions.post("/payments/1/cancel")
                .andExpect(status().is(204));
    }

    @Test
    public void cancelPaymentFailureShouldReturn400() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments/4/cancel"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{ \"code\": \"P0501\", \"description\": \"Cancellation of payment failed\" }")
                        .withHeader("Content-Type", "application/json")
                ));

        restActions.post("/payments/4/cancel")
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
        stubFor(post(urlPathMatching("/v1/payments/refundPaymentId/refunds"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(paymentDetailsWith()
                .serviceId(SERVICE_ID)
                .paymentId("refundPaymentId")
                .applicationReference("refundApplicationReference")
                .amount(100));

        restActions
                .post("/payments/refundApplicationReference/refunds", refundPaymentRequestDtoWith().amount(100).refundAmountAvailable(100).build())
                .andExpect(status().is(201));
    }

    @Test
    public void refundPaymentShouldReturn412IfAmountMismatch() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments/refundPaymentId/refunds"))
                .willReturn(aResponse()
                        .withStatus(412)
                        .withBody("{ \"code\": \"P0604\", \"description\": \"Refund amount available mismatch\" }")
                        .withHeader("Content-Type", "application/json")
                ));

        db.create(paymentDetailsWith()
                .serviceId(SERVICE_ID)
                .paymentId("refundPaymentId")
                .applicationReference("refundApplicationReference")
                .amount(100));

        restActions
                .post("/payments/refundApplicationReference/refunds", refundPaymentRequestDtoWith().amount(100).refundAmountAvailable(100).build())
                .andExpect(status().is(412));
    }

}