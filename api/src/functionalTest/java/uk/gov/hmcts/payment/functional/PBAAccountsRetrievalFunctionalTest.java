package uk.gov.hmcts.payment.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PBAAccountsRetrievalFunctionalTest {

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
            USER_TOKEN = idamService.authenticateUser(testProps.getProbateSolicitorUser(), testProps.getProbateSolicitorPassword());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void retrieveActivePbaAccountDetails() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String accountNumber = testProps.probatePbaAccountNumber;

        // Get pba account by PBA number
        paymentTestService.getPbaAccountDetails(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(OK.value())
            .body("status", equalTo("Active"))
            .body("account_number", equalTo(accountNumber));
    }

    @Test
    public void retrieveOnHoldPbaAccountDetails() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String accountNumber = testProps.onHoldAccountNumber;

        // Get pba account by PBA number
        paymentTestService.getPbaAccountDetails(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(PRECONDITION_FAILED.value())
            .body("status", equalTo("On-Hold"))
            .body("error_message",  equalTo("The account is on hold and temporarily unavailable."));
    }

    @Test
    public void retrieveDeletedPbaAccountDetails() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String accountNumber = testProps.deletedAccountNumber;

        // Get pba account by PBA number
        paymentTestService.getPbaAccountDetails(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(GONE.value())
            .body("status", equalTo("Deleted"))
            .body("error_message",  equalTo("The account has been deleted and is no longer available."));
    }

    @Test
    public void retrieveNonExistedPbaAccountDetails() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

        String accountNumber = testProps.fakeAccountNumber;

        // Get pba account by PBA number
        paymentTestService.getPbaAccountDetails(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(NOT_FOUND.value())
            .body(equalTo("Account not found"));
    }
}
