package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.model.ContactDetails;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
public class RefundsRequestorJourneyPBAFunctionalTest {

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static String USER_TOKEN_PAYMENTS_REFUND_ROLE;
    private static String USER_TOKEN_PAYMENTS_REFUND_APPROVER_ROLE;
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
            USER_TOKEN_PAYMENTS_REFUND_ROLE = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments", "payments-refund").getAuthorisationToken();

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
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        // create a refund request on payment and initiate the refund
        String paymentReference = paymentsResponse.getReference();

        // refund_enable flag should be false before lagTime applied and true after
        Response paymentGroupResponse = paymentTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse groupResponsefromPost = paymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(groupResponsefromPost.getPaymentGroups().get(0).getPayments().get(0).getRefundEnable()).isFalse();

        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            accountPaymentRequest.getCcdCaseNumber(), "5");

        paymentGroupResponse = paymentTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        groupResponsefromPost = paymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(groupResponsefromPost.getPaymentGroups().get(0).getPayments().get(0).getRefundEnable()).isFalse();

        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90.00", "550");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
    }

    @Test
    public void negative_issue_refunds_for_a_pba_payment_unauthorized_user() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        // issue refund with an unauthorised user
        String paymentReference = paymentsResponse.getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90", "0");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_duplicate_issue_refunds_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            accountPaymentRequest.getCcdCaseNumber(), "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        // create a refund request and initiate the refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90.00", "0");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // duplicate the refund
        Response refundResponseDuplicate = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponseDuplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
    }

    @Test
    public void positive_issue_refunds_for_2_pba_payments() {
        // create the PBA payments
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00", "FEE0001",
                "PROBATE", accountNumber);

        String ccdCaseNumber1 = accountPaymentRequest1.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber1, "5");

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00", "FEE0002",
                "PROBATE", "PBAFUNC12345");

        String ccdCaseNumber2 = accountPaymentRequest2.getCcdCaseNumber();

        PaymentDto paymentDto1 = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber2, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto1.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber2);

        // issuing refund using the reference for second payment
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "550.00", "0");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("550.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // Delete payment records
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto1.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
    }


    @Test
    public void positive_issue_refunds_for_a_pba_payment_accross_2_fees() {
        // create a PBA payment with 2 fees
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", accountNumber,
                "FEE0001", "90.00", "FEE002", "550.00");

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentsResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentsResponse.getAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(paymentsResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "640.00", "640");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("640.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // Delete payment records
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
    }

    @Test
    public void positive_add_remission_and_add_refund_for_a_pba_payment() throws JSONException {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentCreationResponse = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
                .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // create retrospective remission
        final String paymentGroupReference = paymentCreationResponse.getPaymentGroupReference();
        final String paymentReference = paymentCreationResponse.getReference();
        Response pbaPaymentStatusesResponse = paymentTestService.getPayments(USER_TOKEN_PAYMENT,SERVICE_TOKEN_PAYMENT,paymentReference);
        String pbaPaymentStatusesResponseString = pbaPaymentStatusesResponse.getBody().asString();
        JSONObject pbaPaymentStatusesResponseJsonObj = new JSONObject(pbaPaymentStatusesResponseString);
        JSONArray jsonArray = pbaPaymentStatusesResponseJsonObj.getJSONArray("fees");

        Integer feeId = null;

        for(int i=0;i<jsonArray.length();i++)
        {
            JSONObject curr = jsonArray.getJSONObject(i);

              feeId = Integer.parseInt(curr.getString("id"));
        }

        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // get payment groups after creating remission and refund
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

         // verify that after adding remission and refund for a payment, addRefund flag should be false
         assertFalse(paymentDtoOptional.get().getRemissions().get(0).isAddRefund());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentCreationResponse.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void checkIssueRefundAddRefundAddRemissionFlagWhenNoBalance(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        // get payment groups after creating full remission and refund
        casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // verify that when there's no available balance, issueRefundAddRefundAddRemission flag should be false
        assertFalse(paymentDtoOptional.get().getPayments().get(0).isIssueRefundAddRefundAddRemission());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void checkIssueRefundAddRefundAddRemissionFlagWithBalance(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("50.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        // get payment groups after creating partial remission and refund
        casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // verify that when there's available balance, issueRefundAddRefundAddRemission flag should be true
        assertThat(paymentDtoOptional.get().getPayments().get(0).isIssueRefundAddRefundAddRemission()==true);

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void amountToBeRefundedIsMoreThanAmountPaidTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "200", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals("The amount you want to refund is more than the amount paid"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void refundAmountFormatTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "50.545", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals("refundAmount: Please check the amount you want to refund"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void refundAmountZeroTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "0", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals("You need to enter a refund amount"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void quantityZeroTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "0", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);
        paymentRefundRequest.getFees().get(0).setUpdatedVolume(3);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals("fees[0].volume: must be greater than 0"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void quantityExceedTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "50", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);
        paymentRefundRequest.getFees().get(0).setUpdatedVolume(3);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals("The quantity you want to refund is more than the available quantity"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void refundShoulBeProductOfFeeAmountAndQuantityTest(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        final Integer feeId = paymentsResponse.getFees().stream().findFirst().get().getId();

        // issue a refund
        String paymentReference = paymentsResponse.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "75", "100");

        paymentRefundRequest.getFees().get(0).setId(feeId);
        paymentRefundRequest.getFees().get(0).setUpdatedVolume(1);

        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        // verify that when Amount to be refunded is more than amount paid
        // throw error - The amount you want to refund is more than the amount paid
        assertThat(refundResponse.getBody().prettyPrint().equals(" \"The Amount to Refund should be equal to the product of Fee Amount and quantity"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void checkRemissionIsAddedButRefundNotSubmitted(){
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("100.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // get payment groups after creating full remission
        casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        paymentDtoOptional
            = paymentGroupResponse.getPaymentGroups().stream().findFirst();
        paymentDtoOptional.get().getRemissions().get(0).setAddRefund(true);

        // verify Given a full/partial remission is added but subsequent refund not submitted, AddRefund flag should be true
        // and issueRefund should be false

        assertFalse(paymentDtoOptional.get().getPayments().get(0).isIssueRefund());
        assertTrue(paymentDtoOptional.get().getRemissions().get(0).isAddRefund());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_add_remission_and_add_refund_for_a_pba_payment_unauthorised_user() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        //create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(FORBIDDEN.value());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_add_remission_and_add_refund_and_add_another_remission_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // test scenario suggests adding a refund therefore adding the refund here
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response addRemissionAgain = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("15.00"), paymentGroupReference, feeId)
            .then().getResponse();

        assertThat(addRemissionAgain.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(addRemissionAgain.getBody().asString()).startsWith("Remission is already exist for FeeId");

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_add_remission_and_add_refund_for_2_pba_payments() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;

        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00", "FEE0001",
                "PROBATE", accountNumber);

        String ccdCaseNumber1 = accountPaymentRequest1.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber1, "5");

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00", "FEE0002",
                "PROBATE", accountNumber);

        String ccdCaseNumber2 = accountPaymentRequest2.getCcdCaseNumber();

        PaymentDto paymentDto1 = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber2, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber2);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        // Delete payment records
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto1.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_add_remission_amount_more_than_fee_amount_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().prettyPrint()).isEqualTo("Hwf Amount should not be more than Fee amount");

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_add_remission_and_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentGroupDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentGroupDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentGroupDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());

        // initiate a refund for the payment
        String paymentReference = paymentDto.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90.00", "0");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
    }

    @Test
    public void positive_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment group
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentGroupDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentGroupDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentGroupDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response retrospectiveRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(retrospectiveRemissionResponse.getStatusCode()).isEqualTo(CREATED.value());

        // submit refund for remission
        String remissionReference = retrospectiveRemissionResponse.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertTrue(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches());

        String paymentReference = paymentDto.getReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "9", "0");
        RefundResponse refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest).then()
                .statusCode(CREATED.value()).extract().as(RefundResponse.class);

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundInitiatedResponse.getRefundReference());
    }

    @Test
    public void positive_create_2_fee_payment_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", accountNumber,
                "FEE0001", "90.00", "FEE0002", "550.00");

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // get payment groups
        Response casePaymentGroupResponse
                = cardTestService
                .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
                = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        Optional<PaymentGroupDto> paymentGroupDtoOptional
                = paymentGroupResponse.getPaymentGroups().stream().findFirst();

        // create retrospective remission
        final String paymentGroupReference = paymentGroupDtoOptional.get().getPaymentGroupReference();
        final Integer feeId = paymentGroupDtoOptional.get().getFees().stream().findFirst().get().getId();
        Response retrospectiveRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("50.00"), paymentGroupReference, feeId)
            .then().getResponse();

        assertThat(retrospectiveRemissionResponse.getStatusCode()).isEqualTo(CREATED.value());

        // submit refund for remission
        String remissionReference = retrospectiveRemissionResponse.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        RetrospectiveRemissionRequest.retrospectiveRemissionRequestWith().remissionReference(remissionReference).build();

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");

        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90", "0");
        RefundResponse refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest).then()
                .statusCode(CREATED.value()).extract().as(RefundResponse.class);

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        // Delete refund record
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundResponseFromPost.getRefundReference());
        paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN,
                refundInitiatedResponse.getRefundReference());
    }

    private void issue_refunds_for_a_failed_payment(final String amount,
                                                    final String accountNumber,
                                                    final String errorMessage) {

        // create a PBA payment
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate(amount,
                "PROBATE", accountNumber);
        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(FORBIDDEN.value());

        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByAccountNumber(USER_TOKEN,
                SERVICE_TOKEN,
                accountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        Optional<PaymentDto> paymentDtoOptional
                = paymentsResponse.getPayments().stream()
                .sorted((s1, s2) -> s2.getDateCreated().compareTo(s1.getDateCreated())).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(paymentDtoOptional.get().getStatus()).isEqualTo("Failed");
        assertThat(paymentDtoOptional.get().getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference, "90", "0");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can be possible if payment is successful");
    }

    @Test
    public void negative_add_remission_and_submit_a_refund_for_a_pba_payment_more_than_the_account_limit() {

        // Create a PBA payment
        this.add_remisssions_and_add_refund_for_a_failed_payment("350000.00",
            "PBAFUNC12345",
            "Payment request failed. PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available");
    }

    @Test
    public void negative_add_remission_and_submit_a_refund_for_a_pba_payment_with_account_deleted() {

        // Create a PBA payment
        this.add_remisssions_and_add_refund_for_a_failed_payment("100.00",
            "PBAFUNC12350",
            "Your account is deleted");
    }

    @Test
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
        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(FORBIDDEN.value());

        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);

        //TEST create retrospective remission
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());

        // Get pba payments by accountNumber
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, accountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        Optional<PaymentDto> paymentDtoOptional
                = paymentsResponse.getPayments().stream()
                .sorted((s1, s2) -> s2.getDateCreated().compareTo(s1.getDateCreated())).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(paymentDtoOptional.get().getStatus()).isEqualTo("Failed");
        assertThat(paymentDtoOptional.get().getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can be possible if payment is successful");
    }

    @Test
    public void negative_issue_refunds_for_a_pba_payment_with_null_contact_details() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).
            then().statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentDtoResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentDtoResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoResponse.getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        // create a refund request on payment and initiate the refund
        String paymentReference = paymentDtoResponse.getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = aRefundRequestWithNullContactDetails("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());
        assertThat(refundResponse.getBody().prettyPrint().equals("contactDetails: Contact Details cannot be null"));

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_issue_refunds_for_a_pba_payment_notification_type_invalid_details() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).
            then().statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentDtoResponse =
                paymentTestService.getPbaPayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentDto.class);

        assertThat(paymentDtoResponse.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoResponse.getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoResponse.getCcdCaseNumber()).isEqualTo(ccdCaseNumber);

        // create a refund request on payment and initiate the refund
        String paymentReference = paymentDtoResponse.getPaymentReference();

        // Validate notification type EMAIL requirements
        List<String> invalidEmailList = Arrays.asList("", "persongmail.com", "person@gmailcom");
        for (String s : invalidEmailList) {

            PaymentRefundRequest paymentRefundRequest
                    = aRefundRequestWithInvalidEmailInContactDetails("RR001", paymentReference, s);
            Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                    SERVICE_TOKEN_PAYMENT,
                    paymentRefundRequest);

            assertThat(refundResponse.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());
        }

        // validate notification type for LETTER has to have postal code populated
        PaymentRefundRequest paymentRefundRequest
            = aRefundRequestWithEmptyPostalCodeInContactDetails("RR001", paymentReference, "");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        // Delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    private static RetroRemissionRequest getRetroRemissionRequest(final String remissionAmount) {
        return RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfAmount(new BigDecimal(remissionAmount))
            .hwfReference("HWF-A1B-23C")
            .build();
    }

    public static PaymentRefundRequest aRefundRequestWithNullContactDetails(final String refundReason,
                                                                            final String paymentReference) {
        return PaymentRefundRequest
            .refundRequestWith().paymentReference(paymentReference)
            .refundReason(refundReason).build();

    }


    public static PaymentRefundRequest aRefundRequestWithInvalidEmailInContactDetails(final String refundReason,
                                                                                      final String paymentReference, String email) {
        return PaymentRefundRequest
            .refundRequestWith().paymentReference(paymentReference)
            .refundReason(refundReason)
            .contactDetails(ContactDetails.contactDetailsWith().
                addressLine("")
                .country("")
                .county("")
                .city("")
                .postalCode("")
                .email(email)
                .notificationType("EMAIL")
                .build())
            .build();

    }

    public static PaymentRefundRequest aRefundRequestWithEmptyPostalCodeInContactDetails(final String refundReason,
                                                                                            final String paymentReference, String postalCode) {
        return PaymentRefundRequest
            .refundRequestWith().paymentReference(paymentReference)
            .refundReason(refundReason)
            .contactDetails(ContactDetails.contactDetailsWith().
                addressLine("")
                .country("")
                .county("")
                .city("")
                .postalCode(postalCode)
                .email("")
                .notificationType("LETTER")
                .build())
            .build();

    }
}
