package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
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

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests"})
public class PaymentStatusFunctionalTest {

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static boolean TOKENS_INITIALIZED = false;

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

            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE =
                idamService.createUserWithSearchScope(CMC_CASE_WORKER_GROUP, "payments-refund")
                    .getAuthorisationToken();
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void positive_bounce_cheque_payment_failure() {

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
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentDto.getReference());

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusBouncedChequeDto.getFailureReference());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return404_bounce_cheque_payment_failure_when_refund_not_found() {

        String accountNumber = testProps.existingAccountNumber;
        String paymentReference = "RC-111-1114-" + RandomUtils.nextInt();
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentReference);
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat("No Payments available for the given Payment reference").isEqualTo(bounceChequeResponse.getBody().prettyPrint());

    }

    @Test
    public void negative_return429_bounce_cheque_payment_failure_when_refund_not_found() {

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
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentDto.getReference());

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDtoNext
            = PaymentFixture.bouncedChequeRequestForFailureRef(paymentDto.getReference(), paymentStatusBouncedChequeDto.getFailureReference());
        Response bounceChequeResponseNext = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDtoNext);
        assertThat(bounceChequeResponseNext.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat("Request already received for this failure reference").isEqualTo(bounceChequeResponseNext.getBody().prettyPrint());
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_chargeback_payment_failure() {

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
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

         assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return404_chargeback_payment_failure_when_refund_not_found() {

        String accountNumber = testProps.existingAccountNumber;
        String paymentReference = "RC-111-1114-" + RandomUtils.nextInt();
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference);
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat("No Payments available for the given Payment reference").isEqualTo(chargebackResponse.getBody().prettyPrint());

    }

    @Test
    public void negative_return429_chargeback_payment_failure_when_refund_not_found() {

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
        PaymentStatusChargebackDto paymentStatusChargebackDtoNext
            = PaymentFixture.chargebackRequestForFailureRef(paymentDto.getReference(), paymentStatusChargebackDto.getFailureReference());
        Response chargebackResponseNext = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDtoNext);
        assertThat(chargebackResponseNext.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat("Request already received for this failure reference").isEqualTo(chargebackResponseNext.getBody().prettyPrint());
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void return_Success_Get_for_payment_failure() {

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
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getPaymentReference()).isEqualTo(paymentStatusChargebackDto.getPaymentReference());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureType()).isEqualTo("Chargeback");
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureEventDateTime()).isEqualTo(paymentStatusChargebackDto.getFailureEventDateTime());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getAdditionalReference()).isEqualTo(paymentStatusChargebackDto.getAdditionalReference());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getHasAmountDebited()).isEqualTo(paymentStatusChargebackDto.getHasAmountDebited());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void return_404_Get_for_payment_failure() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        Response paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN, SERVICE_TOKEN, "RC-1656-9291-1811-2800");
        assertThat("no record found").isEqualTo(paymentsFailureResponse.body().prettyPrint());

        assertThat(paymentsFailureResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }
}
