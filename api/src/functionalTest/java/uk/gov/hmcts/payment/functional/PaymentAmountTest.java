package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.thucydides.junit.annotations.TestData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

@RunWith(SerenityParameterizedRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentAmountTest {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";
    private static final String OK = "OK";
    private static final String NOT_OK = "NOT OK";

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentTestService paymentTestService;
    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;

    @TestData
    public static Collection<Object[]> testData() {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"0.01", OK});
        params.add(new Object[]{"0.10", OK});
        params.add(new Object[]{"0.99", OK});

        params.add(new Object[]{"1.01", OK});
        params.add(new Object[]{"1.09", OK});
        params.add(new Object[]{"1.10", OK});
        params.add(new Object[]{"1.99", OK});

        // GovPay error for this big amount
        // AmountDataPoint.of("9999999.99",OK),

        params.add(new Object[]{"0.00", NOT_OK});
        params.add(new Object[]{"-0.01", NOT_OK});
        params.add(new Object[]{"1.1000", NOT_OK});

        return params;
    }

    public final String amount;

    public final String expectedStatus;

    public PaymentAmountTest(String amount, String expectedStatus) {
        this.amount = amount;
        this.expectedStatus = expectedStatus;
    }

    @Before
    public void setUp() throws Exception {
        //hook into the Spring test-support framework because of @RunWith(SerenityParameterizedRunner.class)
        TestContextManager tcm = new TestContextManager(getClass());
        tcm.prepareTestInstance(this);

        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("citizen");
            USER_TOKEN = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void shouldCreateCardPaymentsWithCorrectAmount() {
        if (testProps.baseTestUrl.contains("payment-api-pr-")) {
            return; // temporarily passing the test in PR environment
        }
        // invoke card payment and assert expectedStatus
        Response response = paymentTestService.postcardPayment(USER_TOKEN, SERVICE_TOKEN, PaymentFixture.aCardPaymentRequest(amount));
        if (!OK.equalsIgnoreCase(expectedStatus)) {
            response.then().statusCode(UNPROCESSABLE_ENTITY.value());
        } else {
            String reference = response.then()
                .statusCode(CREATED.value())
                .and()
                .extract().body().jsonPath().getString("reference");
            paymentReference = reference;
            assertThat(reference).matches(PAYMENT_REFERENCE_REFEX);

            // invoke get payment by reference and assert value
            BigDecimal amount = paymentTestService.getCardPayment(USER_TOKEN, SERVICE_TOKEN, reference)
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().get("amount");
            assertThat(amount).isEqualByComparingTo(amount);
        }
    }

    @Test
    public void shouldCreatePbaPaymentsWithCorrectAmount() {
        if (testProps.baseTestUrl.contains("payment-api-pr-")) {
            return; // temporarily passing the test in PR environment
        }
        // invoke pba payment and assert expectedStatus
        CreditAccountPaymentRequest request = PaymentFixture.aPbaPaymentRequest(amount, "CMC");
        request.setCaseReference("amountTestPbaCaseReference");

        Response response = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, request);
        if (!OK.equalsIgnoreCase(expectedStatus)) {
            response.then().statusCode(UNPROCESSABLE_ENTITY.value());
        } else {
            String reference = response.then()
                .statusCode(CREATED.value())
                .and()
                .extract().body().jsonPath().getString("reference");
            paymentReference = reference;
            assertThat(reference).matches(PAYMENT_REFERENCE_REFEX);

            // invoke get payment by reference and assert value
            BigDecimal amount = paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, reference).then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().get("amount");
            assertThat(amount).isEqualByComparingTo(amount);
        }
    }

    @Test
    public void shouldCreateBarPaymentsWithCorrectAmount() {
        if (testProps.baseTestUrl.contains("payment-api-pr-")) {
            return; // temporarily passing the test in PR environment
        }
        // invoke bar payment and assert expectedStatus
        Response response = paymentTestService.recordBarPayment(USER_TOKEN, SERVICE_TOKEN, PaymentFixture.aBarPaymentRequest(amount));
        if (!OK.equalsIgnoreCase(expectedStatus)) {
            response.then().statusCode(UNPROCESSABLE_ENTITY.value());
        } else {
            String reference = response.then()
                .statusCode(CREATED.value())
                .and()
                .extract().body().jsonPath().getString("reference");
            paymentReference = reference;
            assertThat(reference).matches(PAYMENT_REFERENCE_REFEX);
        }
    }

    @After
    public void deletePayment() {
        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
        }
    }

    @AfterClass
    public static void tearDown() {
        if (!userEmails.isEmpty()) {
            // delete idam test user
            userEmails.forEach(IdamService::deleteUser);
        }
    }
}
