package uk.gov.hmcts.payment.functional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class TelephonyPaymentsTest {
    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createTelephonyPaymentAndExpectSuccess() {
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.google.com")
            .when().createTelephonyPayment(getTelephonyPayment("success"))
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
            assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });
    }

//    Add new functional tests to cover the telephony payments with new pci pal provider with new provider covering following scenarios.
//
//    failed telephony payments
//    success ones
//    Error ones
//

    private PaymentRecordRequest getTelephonyPayment(String status) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .paymentStatus(PaymentStatus.paymentStatusWith().name(status).build())
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference("ref_1234")
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .externalReference("TEL_PAY_1234")
            .siteId("ABCD")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal("99.99"))
                    .code("FEE012345")
                    .reference("ref_1234")
                    .version("1")
                    .volume(1)
                    .build()
                )
            )
            .build();
    }

    @Test
    public void retrieveASuccessfulTelephonyPaymentViaLookup() {
        // payment provider: pci pal
        // payment channel: telephony
        // status: success
        String pbaNumber = "PBA1";

        //create payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.goooooogle.com")
            .when().createTelephonyPayment(getTelephonyPayment("success"))
            .searchPaymentsByPBANumber(pbaNumber)
            .then().got(PaymentsResponse.class, paymentsResponse -> {
            assertEquals("only one payment has been retrieved", 1, paymentsResponse.getPayments().size());
            PaymentDto paymentRetrieved = paymentsResponse.getPayments().get(0);
            assertNotNull(paymentRetrieved.getReference());
            assertEquals("payment status is properly set", "success", paymentRetrieved.getStatus());
        });

        //retrieve payment via lookup
    }

    @Test
    public void retrieveAFailedTelephonyPaymentViaLookup() {
        // payment provider: pci pal
        // payment channel: telephony
        // status: failed
        String pbaNumber = "PBA12";

        //create payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.goooooogle.com")
            .when().createTelephonyPayment(getTelephonyPayment("failed"))
            .searchPaymentsByPBANumber(pbaNumber)
            .then().got(PaymentsResponse.class, paymentsResponse -> {
            assertEquals("only one payment has been retrieved", 1, paymentsResponse.getPayments().size());
            PaymentDto paymentRetrieved = paymentsResponse.getPayments().get(0);
            assertNotNull(paymentRetrieved.getReference());
            assertEquals("payment status is properly set", "failed", paymentRetrieved.getStatus());
        });

        //retrieve payment via lookup
    }

    @Test
    public void retrieveAnErrorneousTelephonyPaymentViaLookup() {
        // payment provider: pci pal
        // payment channel: telephony
        // status: error
        String pbaNumber = "PBA123";

        //create payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.goooooogle.com")
            .when().createTelephonyPayment(getTelephonyPayment("error"))
            .searchPaymentsByPBANumber(pbaNumber)
            .then().got(PaymentsResponse.class, paymentsResponse -> {
            assertEquals("only one payment has been retrieved", 1, paymentsResponse.getPayments().size());
            PaymentDto paymentRetrieved = paymentsResponse.getPayments().get(0);
            assertNotNull(paymentRetrieved.getReference());
            assertEquals("payment status is properly set", "error", paymentRetrieved.getStatus());
            // TODO: check error message
        });

        //retrieve payment via lookup
    }

//    TODO: update the API docs to reflect the new provider. Also support liberata to test these

}
