package uk.gov.hmcts.payment.functional;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import static org.assertj.core.api.Java6Assertions.assertThat;
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

    @Before
    public void setUp() {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.cmcServiceName, testProps.cmcSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void getPbaPaymentsByAccount() {
        // create a PBA payment
        String accountNumber = "PBA234" + RandomUtils.nextInt();
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture.aPbaPaymentRequest("90.00");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest)
            .then()
            .statusCode(CREATED.value());

        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService.getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        assertThat(paymentsResponse.getPayments().size()).isEqualTo(1);
        assertThat(paymentsResponse.getPayments().get(0).getAccountNumber()).isEqualTo(accountNumber);
    }
}
