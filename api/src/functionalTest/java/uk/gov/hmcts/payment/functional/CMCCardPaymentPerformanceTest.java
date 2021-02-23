package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class CMCCardPaymentPerformanceTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;
    @Autowired
    private LaunchDarklyFeature featureToggler;

    @Autowired
    private PaymentTestService paymentTestService;

    private RestTemplate restTemplate;

    @Value("${gov.pay.url}")
    private String govpayUrl;

    @Value("${gov.pay.keys.cmc}")
    private String govpayCmcKey;

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void makeAndRetrieve500CardPaymentsByProbateFromLiberata() {
        // create Card payments
        final Integer PaymentCount = 500;
        final Long responseTime = 30L;
        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        CardPaymentRequest[] cardPaymentRequest = new CardPaymentRequest[PaymentCount];

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            cardPaymentRequest[i] = PaymentFixture.cardPaymentRequestProbate("215.00", Service.PROBATE);

            paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, cardPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Initiated"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get card payments liberate pull with start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrieve800CardPaymentsByProbateFromLiberata() {
        // create Card payments
        final Integer PaymentCount = 800;
        final Long responseTime = 30L;
        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        CardPaymentRequest[] cardPaymentRequest = new CardPaymentRequest[PaymentCount];

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            cardPaymentRequest[i] = PaymentFixture.cardPaymentRequestProbate("215.00", Service.PROBATE);

            paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, cardPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Initiated"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get card payments liberate pull with start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrieve1000CardPaymentsByProbateFromLiberata() {
        // create Card payments
        final Integer PaymentCount = 1000;
        final Long responseTime = 45L;
        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        CardPaymentRequest[] cardPaymentRequest = new CardPaymentRequest[PaymentCount];

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            cardPaymentRequest[i] = PaymentFixture.cardPaymentRequestProbate("215.00", Service.PROBATE);

            paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, cardPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Initiated"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get card payments liberate pull with start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrieve1300CardPaymentsByProbateFromLiberata() {
        // create Card payments
        final Integer PaymentCount = 1300;
        final Long responseTime = 60L;
        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        CardPaymentRequest[] cardPaymentRequest = new CardPaymentRequest[PaymentCount];

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            cardPaymentRequest[i] = PaymentFixture.cardPaymentRequestProbate("215.00", Service.PROBATE);

            paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, cardPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Initiated"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get card payments liberate pull with start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrieveResponseTime30CardPaymentsByProbateFromLiberata() {
        // create Card payments
        final Integer PaymentCount = 30;
        SimpleDateFormat formatter= new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        CardPaymentRequest[] cardPaymentRequest = new CardPaymentRequest[PaymentCount];

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            cardPaymentRequest[i] = PaymentFixture.cardPaymentRequestProbate("215.00", Service.PROBATE);

            paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, cardPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Initiated"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get card payments liberate pull response time
        Response liberataResponse = paymentTestService.getLiberatePullPaymentsTimeByStartAndEndDate(SERVICE_TOKEN, startDate,endDate);
        assertThat(liberataResponse.statusCode()).isEqualTo(200);
        LOG.info("Response time in milliseconds is : {}",liberataResponse.getTime());
    }


}
