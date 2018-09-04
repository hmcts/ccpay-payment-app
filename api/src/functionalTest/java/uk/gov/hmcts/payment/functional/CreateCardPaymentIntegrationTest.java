package uk.gov.hmcts.payment.functional;

import org.apache.commons.lang.RandomStringUtils;
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
import java.util.UUID;

public class CreateCardPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired(required = true)
    private PaymentsV2TestDsl dsl;

    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static String cmcUserId = UUID.randomUUID().toString() + "@hmcts.net";

    private static String cmcUserPassword = RandomStringUtils.random(15, characters);

    private CardPaymentRequest validCardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
        .amount(new BigDecimal("200.11"))
        .caseReference("caseReference")
        .currency(CurrencyCode.GBP)
        .description("Test cross field validation")
        .service(Service.CMC)
        .siteId("siteID")
        .fees(Arrays.asList(FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200.11"))
            .code("FEE0123")
            .version("1")
            .volume(new Double(1))
            .build())).build();


    @Test
    public void createCMCCardPaymentShoudReturn201() {
        dsl.given().userId(cmcUserId, cmcUserPassword, cmcUserRole, cmcUserGroup).serviceId(cmcServiceName, cmcSecret).returnUrl("https://www.google.com")
            .when().createCardPayment(validCardPaymentRequest)
            .then().created(paymentDto -> {
                Assert.assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });
    }
}
