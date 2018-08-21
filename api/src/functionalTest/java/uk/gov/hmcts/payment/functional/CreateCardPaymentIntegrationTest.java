package uk.gov.hmcts.payment.functional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.functional.dsl.PaymentsV2TestDsl;

import java.math.BigDecimal;
import java.util.Arrays;

public class CreateCardPaymentIntegrationTest extends IntegrationTestBase {
    @Autowired
    private PaymentsV2TestDsl dsl;

    private CardPaymentRequest validCardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
        .amount(new BigDecimal("200.11"))
        .ccdCaseNumber("ccdCaseNumber")
        .caseReference("caseReference")
        .currency(CurrencyCode.GBP)
        .description("Test cross field validation")
        .service(Service.CMC)
        .siteId("siteID")
        .fees(Arrays.asList(FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200.11"))
            .code("X0001")
            .version("1")
            .build())).build();

    @Test
    public void validCardPaymentShouldResultIn201() {
        dsl.given().userId(probateUserId).serviceId(probateServiceName, probateSecret).returnUrl("http://www.google.com")
            .when().createCardPayment(validCardPaymentRequest)
            .then().created(paymentDto -> {
                Assert.assertEquals("payment amount is correct", new BigDecimal("200.11"), paymentDto.getAmount());
        });
    }
}
