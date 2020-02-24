package uk.gov.hmcts.payment.api.v1.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto.createPaymentRequestDtoWith;
import static uk.gov.hmcts.payment.api.v1.model.PaymentOld.paymentWith;

public class PaymentsComponentTest extends TestUtil {

    private static final String USER_ID = "3";

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }


    @Before
    public void setup() {
        restActions
                .withAuthorizedService("divorce")
                .withAuthorizedUser(USER_ID);

    }

    @Test
    public void createPaymentValidationRules() throws Exception {
        CreatePaymentRequestDto validRequest = createPaymentRequestDtoWith()
                .amount(100)
                .description("description")
                .reference("reference")
                .returnUrl("https://returnUrl")
                .build();

        tryCreateAndExpect(validRequest.withAmount(null), "amount: must not be null");
        tryCreateAndExpect(validRequest.withAmount(0), "amount: must be greater than or equal to 1");
        tryCreateAndExpect(validRequest.withReference(null), "reference: must not be empty");
        tryCreateAndExpect(validRequest.withReference(""), "reference: must not be empty");
        tryCreateAndExpect(validRequest.withDescription(null), "description: must not be empty");
        tryCreateAndExpect(validRequest.withDescription(""), "description: must not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl(null), "returnUrl: must not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl(""), "returnUrl: must not be empty");
        tryCreateAndExpect(validRequest.withReturnUrl("invalid"), "returnUrl: must be a valid URL");
        tryCreateAndExpect(validRequest.withReturnUrl("http://invalid"), "returnUrl: must be a valid URL");
    }

    private void tryCreateAndExpect(CreatePaymentRequestDto requestBody, String expectedContent) throws Exception {
        restActions
                .post(format("/users/%s/payments", USER_ID), requestBody)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(expectedContent));
    }


    private PaymentOld.PaymentOldBuilder validPaymentWith() {
        return paymentWith()
                .userId(USER_ID)
                .amount(100)
                .status("status")
                .govPayId("paymentId")
                .description("description")
                .email("email@email.com")
                .reference("reference")
                .returnUrl("returnUrl")
                .cancelUrl(resolvePlaceholders("${gov.pay.url}/GOV_PAY_ID/cancel"))
                .refundsUrl(resolvePlaceholders("${gov.pay.url}/GOV_PAY_ID/refunds"))
                .finished(true);
    }

}
