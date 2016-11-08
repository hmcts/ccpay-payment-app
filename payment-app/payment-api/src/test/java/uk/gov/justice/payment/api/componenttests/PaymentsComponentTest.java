package uk.gov.justice.payment.api.componenttests;

import org.junit.Test;
import uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto.StateDto;
import uk.gov.justice.payment.api.json.api.TransactionRecord;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.justice.payment.api.componenttests.sugar.RestActions.SERVICE_ID;
import static uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto.createPaymentRequestDtoWith;
import static uk.gov.justice.payment.api.domain.PaymentDetails.paymentDetailsWith;
import static uk.gov.justice.payment.api.json.api.TransactionRecord.transactionRecordWith;

public class PaymentsComponentTest extends ComponentTestBase {

    @Test
    public void searchPayments() throws Exception {
        db.create(paymentDetailsWith().paymentReference("Ref1").serviceId(SERVICE_ID));
        db.create(paymentDetailsWith().paymentReference("Ref2").serviceId(SERVICE_ID));

        restActions.get("/payments/?payment_reference=Ref1")
                .andExpect(status().isOk())
                .andExpect(bodyAs(TransactionRecord.class).containsExactly(
                        transactionRecordWith().serviceId(SERVICE_ID).paymentReference("Ref1").build()
                ));

        restActions.get("/payments/?payment_reference=Ref999")
                .andExpect(status().isOk())
                .andExpect(bodyAs(TransactionRecord.class).containsExactly());
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
        tryCreateAndExpect(validRequest.withApplicationReference(null), "applicationReference: may not be null");
        tryCreateAndExpect(validRequest.withDescription(null), "description: may not be null");
        tryCreateAndExpect(validRequest.withEmail(null), "email: may not be null");
        tryCreateAndExpect(validRequest.withEmail("invalid@"), "email: not a well-formed email address");
        tryCreateAndExpect(validRequest.withPaymentReference(null), "paymentReference: may not be null");
        tryCreateAndExpect(validRequest.withReturnUrl(null), "returnUrl: may not be null");
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

        db.create(paymentDetailsWith().paymentId("123"));

        restActions.get("/payments/123")
                .andExpect(status().isOk())
                .andExpect(body().isEqualTo(
                        PaymentDto.paymentDtoWith()   // as defined in .json file
                                .state(new StateDto("created", false, null, null))
                                .paymentId("123")
                                .amount(12000)
                                .description("New passport application")
                                .reference("Reference")
                                .createdDate("2016-09-29T09:12:38.413Z")
                                .links(new PaymentDto.LinksDto(
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
                        .withBody(contentsOf("viewPaymentResponse.json"))
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
}