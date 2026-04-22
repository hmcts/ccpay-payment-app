package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.service.TelephonySystem;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.config.ValidUser;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
//@SpringBootTest(classes = {PaymentApiApplication.class})
public class RefundsRequestorJourneyTelephonyPaymentFunctionalTest {

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Pattern REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);

    @Autowired
    private PaymentTestService paymentTestService;

    @Autowired
    private PaymentsTestDsl paymentsTestDsl;

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Inject
    private CaseTestService cardTestService;

    @Autowired
    private PaymentsTestDsl dsl;

    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private String refundReference;

    @Before
    public void setUp() throws Exception {

        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("caseworker-cmc-solicitor");
            USER_TOKEN = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

            ValidUser user2 = idamService.createUserWithSearchScope("payments-refund", "payments-refund-divorce");
            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
            TOKENS_INITIALIZED = true;
        }
    }


    @Test
    public void createTelephonyPaymentAndExpectSuccess() {

        String ccdCaseNumber = "1111221233124419";
        FeeDto feeDto = FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("550.00"))
                .ccdCaseNumber(ccdCaseNumber)
                .version("4")
                .code("FEE0002")
                .volume(1)
                .description("Application for a third party debt order")
                .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest =
                TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
                        .amount(new BigDecimal("550"))
                        .ccdCaseNumber(ccdCaseNumber)
                        .currency(CurrencyCode.GBP)
                        .caseType("DIVORCE")
                         .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
                        .returnURL("https://www.moneyclaims.service.gov.uk")
                        .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(groupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
            assertThat(paymentGroupFeeDto).isNotNull();

            String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

            dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                assertThat(telephonyCardPaymentsResponse).isNotNull();
                assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX))
                        .isTrue();
                assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
            });
            // pci-pal callback
            TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

            dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();
        });

        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
                ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentReference).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);
        int paymentId = paymentsResponse.getFees().get(0).getId();
        // initiate the refund
        PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "550.00", "550");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                SERVICE_TOKEN_PAYMENT,
                paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        refundReference = refundResponseFromPost.getRefundReference();
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundReference).matches())
                .isEqualTo(true);
    }

    @Test
    public void negative_create_telephony_payment_under_lag_time() {

        String ccdCaseNumber = "1111221233124419";
        FeeDto feeDto = FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("550.00"))
                .ccdCaseNumber(ccdCaseNumber)
                .version("4")
                .code("FEE0002")
                .description("Application for a third party debt order")
                .volume(1)
                .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest =
                TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
                        .amount(new BigDecimal("550"))
                        .ccdCaseNumber(ccdCaseNumber)
                        .currency(CurrencyCode.GBP)
                        .caseType("DIVORCE")
                         .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
                        .returnURL("https://www.moneyclaims.service.gov.uk")
                        .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(groupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
            assertThat(paymentGroupFeeDto).isNotNull();

            String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

            dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                assertThat(telephonyCardPaymentsResponse).isNotNull();
                assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX))
                        .isTrue();
                assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
            });
            // pci-pal callback
            TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

            dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();});


        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "4");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentReference).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);
        int paymentId = paymentsResponse.getFees().get(0).getId();
        // initiate the refund
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "550", "550");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().asString()).isEqualTo("This payment is not yet eligible for refund");
    }

    @After
    public void deletePayment() {
        if (refundReference != null) {
            //delete refund record
            paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN, refundReference);
        }

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
