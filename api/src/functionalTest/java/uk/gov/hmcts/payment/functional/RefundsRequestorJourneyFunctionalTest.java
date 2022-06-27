package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
// @Ignore("Only to be activated after Refunds V1 is merged to master and that is the test that should be enabled.")
public class RefundsRequestorJourneyFunctionalTest {

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Pattern REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");

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

    @Before
    public void setUp() throws Exception {

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor")
                .getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

            USER_TOKEN_CMC_CITIZEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();

            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE =
                idamService.createUserWithSearchScope(CMC_CASE_WORKER_GROUP, "payments-refund")
                    .getAuthorisationToken();
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void positive_issue_refunds_for_a_pba_payment() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_issue_refunds_for_a_pba_payment_unauthorized_user() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        String paymentReference = paymentsResponse.getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_duplicate_issue_refunds_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        Response refundResponseDuplicate = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponseDuplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_issue_refunds_for_2_pba_payments() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00","FEE0001",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest1.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00","FEE0002",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest2.setAccountNumber(accountNumber);
        PaymentDto paymentDto1 = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto1.getReference());
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto1.getReference()).then().statusCode(NO_CONTENT.value());
    }


    @Test
    public void positive_issue_refunds_for_a_pba_payment_accross_2_fees() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", "PBAFUNC12345",
                "FEE0001","90.00","FEE002","550.00");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_issue_refund_for_card_payment() {

        PaymentDto paymentDto = paymentsTestDsl.given().userToken(USER_TOKEN_CMC_CITIZEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.aCardPaymentRequest("20.99"))
            .then().getByStatusCode(201);

        String paymentReference = paymentDto.getReference();
        assertNotNull(paymentReference);
        assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund currently supported for PBA Payment Channel only");
    }

    @Test
    public void positive_add_remission_and_add_refund_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_and_add_refund_for_a_pba_payment_unauthorised_user() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(FORBIDDEN.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_and_add_refund_and_a_duplicate_refund_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        Response refundResponseDuplicate = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponseDuplicate.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponseDuplicate.getBody().asString()).isEqualTo("Refund is already requested for this payment");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_and_add_refund_and_a_duplicate_remission_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response addRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("15.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(addRemissionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(addRemissionResponse.getBody().asString()).startsWith("Remission is already exist for FeeId");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_and_add_refund_for_2_pba_payments() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00","FEE0001",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest1.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00","FEE0002",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest2.setCcdCaseNumber(accountPaymentRequest1.getCcdCaseNumber());
        accountPaymentRequest2.setAccountNumber(accountNumber);
        PaymentDto paymentDto1 = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest1.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto1.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_add_remission_amount_more_than_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().prettyPrint()).isEqualTo("Hwf Amount should not be more than Fee amount");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_and_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response retrospectiveRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(retrospectiveRemissionResponse.getStatusCode()).isEqualTo(CREATED.value());
        String remissionReference = retrospectiveRemissionResponse.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundInitiatedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_create_2_fee_payment_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", "PBAFUNC12345",
                "FEE0001","90.00","FEE0002","550.00");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().stream().filter(s -> s.getCode().equals("FEE0002")).findFirst().get().getId();

        //TEST create retrospective remission
        Response retrospectiveRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("540.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(retrospectiveRemissionResponse.getStatusCode()).isEqualTo(CREATED.value());
        String remissionReference = retrospectiveRemissionResponse.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("540.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void positive_issue_refunds_for_a_failed_pba_payment() {
        issue_refunds_for_a_failed_payment("350000.00", "PBAFUNC12345",
            "Payment request failed. PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available");
    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void positive_issue_refunds_for_a_pba_account_deleted_payment() {
        issue_refunds_for_a_failed_payment("100.00", "PBAFUNC12350", "Your account is deleted");
    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void positive_issue_refunds_for_a_pba_account_on_hold_payment() {
        issue_refunds_for_a_failed_payment("100.00", "PBAFUNC12355", "Your account is on hold");
    }

    private void issue_refunds_for_a_failed_payment(final String amount,
                                                    final String accountNumber,
                                                    final String errorMessage) {

        // create a PBA payment
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate(amount,
                "PROBATE", accountNumber);
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(FORBIDDEN.value()).extract().as(PaymentDto.class);

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        assertThat(paymentsResponse.getStatus()).isEqualTo("Failed");
        assertThat(paymentsResponse.getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        String paymentReference = paymentsResponse.getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can be possible if payment is successful");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void negative_add_remission_and_submit_a_refund_for_a_pba_payment_more_than_the_account_limit() {

        // Create a PBA payment
        this.add_remisssions_and_add_refund_for_a_failed_payment("350000.00",
            "PBAFUNC12345",
            "Payment request failed. PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available");
    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void negative_add_remission_and_submit_a_refund_for_a_pba_payment_with_account_deleted() {

        // Create a PBA payment
        this.add_remisssions_and_add_refund_for_a_failed_payment("100.00",
            "PBAFUNC12350",
            "Your account is deleted");
    }

    @Test
    @Ignore("This test is Ignored as the liberataMock profile would be switched off in AAT")
    public void negative_add_remission_and_submit_a_refund_for_a_pba_payment_with_account_on_hold() {

        // Create a PBA payment
        this.add_remisssions_and_add_refund_for_a_failed_payment("100.00",
            "PBAFUNC12355",
            "Your account is on hold");
    }

    private void add_remisssions_and_add_refund_for_a_failed_payment(final String amount,
                                                                     final String accountNumber,
                                                                     final String errorMessage) {
        // Create a PBA payment
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate(amount,
                "PROBATE", accountNumber);
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(FORBIDDEN.value()).extract().as(PaymentDto.class);

        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        assertThat(paymentsResponse.getStatus()).isEqualTo("Failed");
        assertThat(paymentsResponse.getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can be possible if payment is successful");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void issue_add_remission_and_add_refund_for_a_failed_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response refundResponseFail = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());

        assertThat(refundResponseFail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponseFail.getBody().print()).isEqualTo("Refund can't be requested for failed payment");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void issue_refunds_for_a_failed_payment() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentRefundRequest paymentRefundRequest1
            = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
        Response refundResponseFail = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest1);

        assertThat(refundResponseFail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponseFail.getBody().print()).isEqualTo("Refund can't be requested for failed payment");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    private static final RetroRemissionRequest getRetroRemissionRequest(final String remissionAmount) {
        return RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfAmount(new BigDecimal(remissionAmount))
            .hwfReference("HWF-A1B-23C")
            .build();
    }
}
