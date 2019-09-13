package uk.gov.hmcts.payment.functional;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PBAPaymentFunctionalTest {

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

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";

    @Before
    public void setUp() {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void makeAndRetrievePbaPaymentsByCMC() {
        // create a PBA payment
        String accountNumber = "PBA234" + RandomUtils.nextInt();
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture.aPbaPaymentRequest("90.00", Service.CMC);
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest)
            .then()
            .statusCode(CREATED.value())
            .body("status", equalTo("Pending"));

        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService.getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(paymentsResponse.getPayments().size()).isEqualTo(1);
        assertThat(paymentsResponse.getPayments().get(0).getAccountNumber()).isEqualTo(accountNumber);
    }

    //@Test
    public void makeAndRetrievePbaPaymentByFinremLiberataCheckOn() {
        System.out.println("Service.FINREM.getName(): " + Service.FINREM.getName());
        System.out.println("testProps.existingAccountNumber: " + testProps.existingAccountNumber);

        String startDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT);

        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture.aPbaPaymentRequest("90.00", Service.FINREM);
        accountPaymentRequest.setAccountNumber(testProps.existingAccountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest)
            .then()
            .statusCode(CREATED.value())
            .body("status", equalTo("Success"));

        String endDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsByServiceBetweenDates(Service.FINREM, startDate, endDate)
            .then().getPayments((paymentsResponse -> {
            Assertions.assertThat(paymentsResponse.getPayments().size()).isEqualTo(1);
        }));
    }

    @Test
    public void shouldRejectDuplicatePayment() {
        String accountNumber = "PBA333" + RandomUtils.nextInt();
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture.aPbaPaymentRequest("550.50", Service.CMC);
        accountPaymentRequest.setAccountNumber(accountNumber);
        // when & then
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest)
            .then()
            .statusCode(CREATED.value())
            .body("status", equalTo("Pending"));

        // duplicate payment with same details from same user
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest)
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(equalTo("duplicate payment"));
    }
}
