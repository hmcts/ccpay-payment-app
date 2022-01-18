package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.junit.Before;
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
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
//@SpringBootTest(classes = {PaymentApiApplication.class})
public class RefundsRequestorJourneyPBAFunctionalTest {

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
            System.out.println("The value of the USER_TOKEN : "+USER_TOKEN);

            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            System.out.println("The value of the SERVICE_TOKEN : "+SERVICE_TOKEN);

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
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).
            then().statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

        // get the payment by ccdCaseNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);

        // create a refund request on payment and initiate the refund
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        System.out.println(refundResponseFromPost.getRefundReference());
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);
    }

    @Test
    public void negative_issue_refunds_for_a_pba_payment_unauthorized_user() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

        // get the payment by ccdCaseNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);

        // issue refund with an unauthorised user
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    public void negative_duplicate_issue_refunds_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            accountPaymentRequest.getCcdCaseNumber(),"5");

        // get the payments by ccdCaseNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);


        // create a refund request and initiate the refund
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // duplicate the refund
        Response refundResponseDuplicate = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        assertThat(refundResponseDuplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void positive_issue_refunds_for_2_pba_payments() {
        // create the PBA payments
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00", "FEE0001",
                "PROBATE", accountNumber);

        String ccdCaseNumber1 = accountPaymentRequest1.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber1,"5");

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00", "FEE0002",
                "PROBATE", "PBAFUNC12345");

        String ccdCaseNumber2 = accountPaymentRequest2.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber2,"5");

        // get the payments by ccdCaseNumbers
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber2)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber2);
        System.out.println("The value of the CCD Case Number for the second payment " + ccdCaseNumber2);

        // issuing refund using the reference for second payment
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("550.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);
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

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");


        // get the payment by ccdCaseNumber
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);

        // issue a refund
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundResponse.getStatusLine());
        System.out.println(refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);
    }

    @Test
    public void positive_add_remission_and_add_refund_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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

        // submit refund for remission
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);
    }

    @Test
    public void negative_add_remission_and_add_refund_for_a_pba_payment_unauthorised_user() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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
    }

    @Test
    public void negative_add_remission_and_add_refund_and_a_duplicate_refund_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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
        String remissionReference = response.getBody().jsonPath().getString("remission_reference");

        // submit refund on created remission
        RetrospectiveRemissionRequest retrospectiveRemissionRequest
            = PaymentFixture.aRetroRemissionRequest(remissionReference);
        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // issue a duplicate refund on the same remission
        Response refundResponseDuplicate = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT, retrospectiveRemissionRequest);

        assertThat(refundResponseDuplicate.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(refundResponseDuplicate.getBody().asString()).isEqualTo("Refund is already requested for this payment");
    }

    @Test
    public void negative_add_remission_and_add_refund_and_add_another_remission_for_a_pba_payment() {
        // Create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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
    }

    @Test
    public void positive_add_remission_and_add_refund_for_2_pba_payments() {

        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;

        CreditAccountPaymentRequest accountPaymentRequest1 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("90.00", "FEE0001",
                "PROBATE", accountNumber);

        String ccdCaseNumber1 = accountPaymentRequest1.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest1).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber1,"5");

        CreditAccountPaymentRequest accountPaymentRequest2 = PaymentFixture
            .aPbaPaymentRequestForProbateWithFeeCode("550.00", "FEE0002",
                "PROBATE", accountNumber);

        String ccdCaseNumber2 = accountPaymentRequest2.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest2).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber2,"5");

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
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);
    }

    @Test
    public void negative_add_remission_amount_more_than_fee_amount_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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
    }

    @Test
    public void positive_add_remission_and_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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

        // get pba payment to initiate a refund for
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);


        // initiate a refund for the payment
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
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
    public void negative_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        // create a PBA payment
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", accountNumber);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        // Get pba payments to initiate a refund for
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("90.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);

        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundInitiatedResponse.getStatusLine());
        System.out.println(refundInitiatedResponse.getBody().prettyPrint());
        assertThat(refundInitiatedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // WHAT IS THE ACCEPTANCE CRITERIA IN THIS TEST? ASSERT THE MESSAGE HERE
    }

    // WHAT IS THE DIFFERENCE COMPARED TO ABOVE SCENARIO OTHER THAN HAVING 2_FEE. IS THIS A POSITIVE OR NEGATIVE CASE?
    @Test
    public void positive_create_2_fee_payment_add_remission_add_refund_and_then_initiate_a_refund_for_a_pba_payment() {
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", accountNumber,
                "FEE0001", "90.00", "FEE0002", "550.00");

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();

        paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success"));
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber,"5");

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

        System.out.println("The value of the responseBody : " + refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(refundResponseFromPost.getRefundReference()).startsWith("RF-");


        // DON'T WE NEED THE BELOW BLOCK IN THIS SCENARIO ???
        // Get pba payments to initiate a refund for
        PaymentsResponse paymentsResponse = paymentTestService
            .getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, ccdCaseNumber)
            .then()
            .statusCode(OK.value()).extract().as(PaymentsResponse.class);
        Optional<PaymentDto> paymentDtoOptional
            = paymentsResponse.getPayments().stream().findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal("640.00"));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);

        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundInitiatedResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        System.out.println(refundInitiatedResponse.getStatusLine());
        System.out.println(refundInitiatedResponse.getBody().prettyPrint());
        assertThat(refundInitiatedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // WHAT IS THE ACCEPTANCE CRITERIA IN THIS TEST? ASSERT THE MESSAGE HERE

    }

    @Test
    public void positive_issue_refunds_for_a_failed_pba_payment() {
        issue_refunds_for_a_failed_payment("350000.00", "PBAFUNC12345",
            "Payment request failed. PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available");
    }

    @Test
    public void positive_issue_refunds_for_a_pba_account_deleted_payment() {
        issue_refunds_for_a_failed_payment("100.00", "PBAFUNC12350", "Your account is deleted");
    }

    @Test
    public void positive_issue_refunds_for_a_pba_account_on_hold_payment() {
        issue_refunds_for_a_failed_payment("100.00", "PBAFUNC12355", "Your account is on hold");
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
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

        assertThat(paymentDtoOptional.get().getAccountNumber()).isEqualTo(accountNumber);
        assertThat(paymentDtoOptional.get().getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(paymentDtoOptional.get().getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
        assertThat(paymentDtoOptional.get().getStatus()).isEqualTo("Failed");
        assertThat(paymentDtoOptional.get().getStatusHistories().get(0).getErrorMessage()).isEqualTo(errorMessage);

        System.out.println("The value of the CCD Case Number " + ccdCaseNumber);
        String paymentReference = paymentDtoOptional.get().getPaymentReference();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest("RR001", paymentReference);
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        System.out.println("The value of the response body " + refundResponse.getBody().prettyPrint());
        assertThat(refundResponse.getBody().print()).isEqualTo("Refund can not be processed for unsuccessful payment");
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
            = paymentsResponse.getPayments().stream().sorted((s1, s2) -> {
            return s2.getDateCreated().compareTo(s1.getDateCreated());
        }).findFirst();

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
