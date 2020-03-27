package uk.gov.hmcts.payment.functional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class CMCCardPaymentFunctionalTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    private RestTemplate restTemplate;

    @Value("${gov.pay.url}")
    private String govpayUrl;

    @Value("${gov.pay.keys.cmc}")
    private String govpayCmcKey;

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
    public void createCMCCardPaymentTestShouldReturn201Success() {
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
            assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });

    }

    @Test
    public void createCMCCardPaymentWithoutFeesTestShouldReturn201Success() {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("29.34"))
            .description("New passport application")
            .caseReference("aCaseReference")
            .service(Service.CMC)
            .currency(CurrencyCode.GBP)
            .siteId("AA101")
            .build();
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(cardPaymentRequest)
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
            assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });
    }


    @Test
    public void retrieveCMCCardPaymentTestShouldReturn200Success() {
        final String[] reference = new String[1];
        // create card payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(savedPayment -> {
            reference[0] = savedPayment.getReference();

            assertNotNull(savedPayment.getReference());
            assertEquals("payment status is properly set", "Initiated", savedPayment.getStatus());
        });


        // retrieve card payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(reference[0])
            .then().get();

        assertNotNull(paymentDto);
        assertEquals(paymentDto.getAmount(), new BigDecimal("20.99"));
        assertEquals(paymentDto.getReference(), reference[0]);
        assertEquals(paymentDto.getExternalProvider(), "gov pay");
        assertEquals(paymentDto.getServiceName(), "Civil Money Claims");
        assertEquals(paymentDto.getStatus(), "Initiated");
        paymentDto.getFees().stream().forEach(f -> {
            assertEquals(f.getVersion(), "1");
            assertEquals(f.getCalculatedAmount(), new BigDecimal("20.99"));
        });

    }


    @Test
    public void retrieveAndValidatePayhubPaymentReferenceFromGovPay() throws Exception {
        final String[] reference = new String[1];

        // create card payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(savedPayment -> {
            reference[0] = savedPayment.getReference();

            assertNotNull(savedPayment.getReference());
            assertEquals("payment status is properly set", "Initiated", savedPayment.getStatus());
        });

        // retrieve govpay reference for the payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(reference[0])
            .then().get();


        /**
         *
         * Retrieve the payment details from govpay, and validate the payhub payment reference.
         */
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + govpayCmcKey);
        HttpEntity<String> httpEntity = new HttpEntity<>("parameters", headers);

        restTemplate = new RestTemplate();

        ResponseEntity<GovPayPayment> res = restTemplate.exchange(govpayUrl + "/" + paymentDto.getExternalReference(),
            HttpMethod.GET, httpEntity, GovPayPayment.class);
        GovPayPayment govPayPayment = res.getBody();

        assertNotNull(govPayPayment);
        assertEquals(govPayPayment.getReference(), paymentDto.getReference());
        assertEquals(govPayPayment.getPaymentId(), paymentDto.getExternalReference());
    }

    private CardPaymentRequest getCardPaymentRequest() {
        return PaymentFixture.aCardPaymentRequest("20.99");
    }

}
