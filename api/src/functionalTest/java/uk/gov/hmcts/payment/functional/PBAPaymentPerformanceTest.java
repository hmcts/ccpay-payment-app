package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.text.SimpleDateFormat;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PBAPaymentPerformanceTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private PaymentTestService paymentTestService;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;
    @Autowired
    private LaunchDarklyFeature featureToggler;

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    private static final String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);



   @Before
    public void setUp() {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor").getAuthorisationToken();
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void makeAndRetrievePba500PaymentsByProbateFromLiberata()  {
        // create a PBA payment
        final Integer PaymentCount = 500;
        final Long responseTime = 30L;
        SimpleDateFormat formatter= new SimpleDateFormat(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest[] accountPaymentRequest = new CreditAccountPaymentRequest[PaymentCount];
            String accountNumber = testProps.existingAccountNumber;

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            accountPaymentRequest[i] = PaymentFixture.aPbaPaymentRequestForProbateForSuccessLiberataValidation("215.00", Service.PROBATE);
            accountPaymentRequest[i].setAccountNumber(accountNumber);
            paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Success"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

       // Get pba payments liberate pull by start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
        .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
   }

    @Test
    public void makeAndRetrievePba800PaymentsByProbateFromLiberata() {
        // create a PBA payment
        final Integer PaymentCount = 800;
        final Long responseTime = 45L;
        SimpleDateFormat formatter= new SimpleDateFormat(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest[] accountPaymentRequest = new CreditAccountPaymentRequest[PaymentCount];
        String accountNumber = testProps.existingAccountNumber;

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            accountPaymentRequest[i] = PaymentFixture.aPbaPaymentRequestForProbateForSuccessLiberataValidation("215.00", Service.PROBATE);
            accountPaymentRequest[i].setAccountNumber(accountNumber);
            paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Success"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get pba payments liberate pull by start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
           .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrievePba1000PaymentsByProbateFromLiberata() {
        // create a PBA payment
        final Integer PaymentCount = 1000;
        final Long responseTime = 60L;
        SimpleDateFormat formatter= new SimpleDateFormat(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest[] accountPaymentRequest = new CreditAccountPaymentRequest[PaymentCount];
        String accountNumber = testProps.existingAccountNumber;

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            accountPaymentRequest[i] = PaymentFixture.aPbaPaymentRequestForProbateForSuccessLiberataValidation("215.00", Service.PROBATE);
            accountPaymentRequest[i].setAccountNumber(accountNumber);
            paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Success"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get pba payments liberate pull by start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate, responseTime)
           .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrievePba1300PaymentsByProbateFromLiberata() {
        // create a PBA payment
        final Integer PaymentCount = 1300;
        final Long responseTime = 60L;
        SimpleDateFormat formatter= new SimpleDateFormat(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest[] accountPaymentRequest = new CreditAccountPaymentRequest[PaymentCount];
        String accountNumber = testProps.existingAccountNumber;

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            accountPaymentRequest[i] = PaymentFixture.aPbaPaymentRequestForProbateForSuccessLiberataValidation("215.00", Service.PROBATE);
            accountPaymentRequest[i].setAccountNumber(accountNumber);
            paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Success"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get pba payments liberate pull by start and end date
        PaymentsResponse liberataResponse = paymentTestService.getLiberatePullPaymentsByStartAndEndDate(SERVICE_TOKEN, startDate,endDate,responseTime)
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(PaymentCount);
    }

    @Test
    public void makeAndRetrieveResponseTime30PbaPaymentsByProbateFromLiberata() {
        // create PBA payments
        final Integer PaymentCount = 30;
        SimpleDateFormat formatter= new SimpleDateFormat(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest[] accountPaymentRequest = new CreditAccountPaymentRequest[PaymentCount];
        String accountNumber = testProps.existingAccountNumber;

        String startDate = formatter.format(LocalDateTime.now().toDate());

        for(int i=0; i<PaymentCount;i++) {
            accountPaymentRequest[i] = PaymentFixture.aPbaPaymentRequestForProbateForSuccessLiberataValidation("215.00", Service.PROBATE);
            accountPaymentRequest[i].setAccountNumber(accountNumber);
            paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest[i])
                .then()
                .statusCode(CREATED.value())
                .body("status", equalTo("Success"));
        }

        String endDate = formatter.format(LocalDateTime.now().toDate());

        // Get pba payments liberate pull response time
        Response liberataResponse = paymentTestService.getLiberatePullPaymentsTimeByStartAndEndDate(SERVICE_TOKEN, startDate,endDate);
        assertThat(liberataResponse.statusCode()).isEqualTo(200);
        LOG.info("Response time in milliseconds is : {}",liberataResponse.getTime());
    }

}
