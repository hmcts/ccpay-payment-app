package uk.gov.hmcts.payment.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.factory.DefaultJackson2ObjectMapperFactory;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.RetroRemissionDto;
import uk.gov.hmcts.payment.api.dto.RetroRemissionRequest;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
public class RefundsRequestorJourneyFunctionalTest {

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static boolean TOKENS_INITIALIZED = false;
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";

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
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void test_positive_issue_refunds_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));

        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, testProps.existingAccountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        System.out.println("The value of the CCD Case Number " + paymentDtoOptional.get().getCcdCaseNumber());
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");
    }

    @Test
    public void test_negative_issue_refund_for_card_payment() {

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
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        //assertThat(refundResponse.getBody().print()).isEqualTo("Refund currently supported for PBA Payment Channel only");
    }

    @Test
    public void test_positive_add_remission_and_add_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        Response casePaymentGroupResponse
            = cardTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
       PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        //System.out.println("The Payment Group Reference : " + casePaymentGroupResponse.getBody().prettyPrint());
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
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");
    }

    @Test
    public void test_positive_add_remission_amount_more_than_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        Response casePaymentGroupResponse
            = cardTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        //System.out.println("The Payment Group Reference : " + casePaymentGroupResponse.getBody().prettyPrint());
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().prettyPrint()).isEqualTo("Hwf Amount should not be more than Fee amount");
    }

    @Test
    public void test_positive_add_remission_and_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));

        Response casePaymentGroupResponse
            = cardTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        //System.out.println("The Payment Group Reference : " + casePaymentGroupResponse.getBody().prettyPrint());
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, testProps.existingAccountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        System.out.println("The value of the CCD Case Number " + paymentDtoOptional.get().getCcdCaseNumber());
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");
    }

    @Test
    public void test_positive_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));

        Response casePaymentGroupResponse
            = cardTestService.getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN_PAYMENT, accountPaymentRequest.getCcdCaseNumber());
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        //System.out.println("The Payment Group Reference : " + casePaymentGroupResponse.getBody().prettyPrint());
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response retrospectiveRemissionResponse = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(retrospectiveRemissionResponse.getStatusCode()).isEqualTo(CREATED.value());
        String remissionReference = retrospectiveRemissionResponse.getBody().jsonPath().getString("remission_reference");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            RetroSpectiveRemissionRequest.retroSpectiveRemissionRequestWith().remissionReference(remissionReference).build());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");


        // Get pba payments by accountNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByAccountNumber(USER_TOKEN, SERVICE_TOKEN, testProps.existingAccountNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);

        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        System.out.println("The value of the CCD Case Number " + paymentDtoOptional.get().getCcdCaseNumber());
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        System.out.println(refundInitiatedResponse.getStatusLine());
        System.out.println(refundInitiatedResponse.getBody().prettyPrint());
        assertThat(refundInitiatedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void test_positive_issue_refunds_for_a_failed_pba_payment() {
        issue_refunds_for_a_failed_payment("350000.00", "PBAFUNC12345",
            "Payment request failed. PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available");
    }

    @Test
    public void test_positive_issue_refunds_for_a_pba_account_deleted_payment() {
        issue_refunds_for_a_failed_payment("100.00", "PBAFUNC12350", "Your account is deleted");
    }

    @Test
    public void test_positive_issue_refunds_for_a_pba_account_on_hold_payment() {
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
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(accountPaymentRequest.getCcdCaseNumber());
        assertThat(paymentDtoOptional.get().getStatus()).isEqualTo("Failed");
        assertThat(paymentDtoOptional.get().getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        System.out.println("The value of the CCD Case Number " + paymentDtoOptional.get().getCcdCaseNumber());
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        System.out.println("The value of the response body " + refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can be possible if payment is successful");
    }

    private static final RetroRemissionRequest getRetroRemissionRequest(final String remissionAmount) {
        return RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfAmount(new BigDecimal(remissionAmount))
            .hwfReference("HWF-A1B-23C")
            .build();
    }
}
