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
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
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
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_bounce_cheque_payment_failure_when_refund_not_found() {

        String accountNumber = testProps.existingAccountNumber;
        String paymentReference = "RC-111-1114-" + RandomUtils.nextInt();
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
            = PaymentFixture.bouncedChequeRequest(paymentReference);

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

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

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_chargeback_payment_failure_when_refund_not_found() {

        String accountNumber = testProps.existingAccountNumber;
        String paymentReference = "RC-111-1114-" + RandomUtils.nextInt();
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
            = PaymentFixture.chargebackRequest(paymentReference);

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

}
