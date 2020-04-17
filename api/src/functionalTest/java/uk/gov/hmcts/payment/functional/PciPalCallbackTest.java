package uk.gov.hmcts.payment.functional;

import com.mifmif.common.regex.Generex;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
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
public class PciPalCallbackTest {
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
    public void updateTelephonyPayment_shouldReturnSucceess() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);

        // create telephony payment using old api
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().getByStatusCode(201);

        assertNotNull(paymentDto.getReference());

        String paymentReference = paymentDto.getReference();

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference(paymentReference)
            .orderAmount("9999")
            .transactionResult("SUCCESS")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().noContent();

        // retrieve payment
        paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(paymentReference)
            .then().get();

        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), paymentReference);
        assertEquals(paymentDto.getExternalProvider(), "pci pal");
        assertEquals(paymentDto.getStatus(), "Success");
    }

    @Test
    public void updateTelephonyPayment_shouldReturnNotFoundWhenPaymentReferenceNotFound() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);

        // create telephony payment using old api
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
        });

        //pci-pal callback
        TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
            .orderReference("RC-Invalid-reference")
            .orderAmount("9999")
            .transactionResult("SUCCESS")
            .build();

        dsl.given().s2sToken(SERVICE_TOKEN)
            .when().telephonyCallback(callbackDto)
            .then().notFound();
    }

    private PaymentRecordRequest getTelephonyPayment(String reference) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference(reference)
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .externalReference(reference)
            .siteId("AA01")
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
            .reportedDateOffline(DateTime.now().toString())
            .build();
    }
}
