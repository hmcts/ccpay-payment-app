package uk.gov.hmcts.payment.functional;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.model.ContactDetails;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
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
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests"})
public class PaymentStatusFunctionalTest {

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static String SERVICE_TOKEN_PAYMENT;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static boolean TOKENS_INITIALIZED = false;
    private static String USER_TOKEN_PAYMENT;
    private static String USER_TOKEN_CARD_PAYMENT;
    private static final Pattern REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");
    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusFunctionalTest.class);

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

    private static DateTimeZone zoneUTC = DateTimeZone.UTC;

    @Before
    public void setUp() throws Exception {

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor")
                .getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE =
                idamService.createUserWithSearchScope(CMC_CASE_WORKER_GROUP, "payments-refund")
                    .getAuthorisationToken();
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
            TOKENS_INITIALIZED = true;

            USER_TOKEN_CARD_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
        }
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

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);

        // issue a refund
        String paymentReference = paymentsResponse.getPaymentReference();
        int paymentId = paymentsResponse.getFees().get(0).getId();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "90", "90");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
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
    public void negative_return404_chargeback_payment_failure_when_payment_not_found() {

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
        assertThat(chargebackResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given Payment reference");

    }

    @Test
    public void negative_return429_chargeback_payment_failure_when_duplicate_event() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("90.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);

        // issue a refund
        String paymentReference = paymentsResponse.getPaymentReference();
        int paymentId = paymentsResponse.getFees().get(0).getId();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "90", "90");
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
        assertThat(chargebackResponseNext.getBody().prettyPrint()).isEqualTo(
                "Request already received for this failure reference");
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

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);

        // issue a refund
        String paymentReference = paymentsResponse.getPaymentReference();
        int paymentId = paymentsResponse.getFees().get(0).getId();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "90", "90");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getPaymentReference()).isEqualTo(paymentStatusChargebackDto.getPaymentReference());
        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureType()).isEqualTo("Chargeback");
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
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, "RC-1656-9291-1811-2800");
        assertThat(paymentsFailureResponse.body().prettyPrint()).isEqualTo("no record found");

        assertThat(paymentsFailureResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_paymentStatusSecond_pba() {

        // Create a Payment By Account
        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
                .aPbaPaymentRequestForProbate("90.00",
                        "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
                .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        // Create a Refund on same payment
        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);

        // issue a refund
        String paymentReference = paymentsResponse.getPaymentReference();
        int paymentId = paymentsResponse.getFees().get(0).getId();
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "90", "90");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                SERVICE_TOKEN_PAYMENT,
                paymentRefundRequest);

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
                = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
                paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate(actualDateTime.plusMinutes(15).toString())
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusSecond_bulk_scan() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "3456908723459901" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(new BigDecimal("100.00"))
                .service("DIVORCE")
                .siteId("AA01")
                .currency(CurrencyCode.GBP)
                .documentControlNumber(dcn)
                .ccdCaseNumber(ccdCaseNumber)
                .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
                .payerName("CCD User1")
                .bankedDate(DateTime.now().toString())
                .paymentMethod(PaymentMethodType.CHEQUE)
                .paymentStatus(PaymentStatus.SUCCESS)
                .giroSlipNo("GH716376")
                .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Collections.singletonList(FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("450.00"))
                        .code("FEE3132")
                        .version("1")
                        .reference("testRef1")
                        .volume(2)
                        .ccdCaseNumber(ccdCaseNumber)
                        .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(paymentGroupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
            assertThat(paymentGroupFeeDto).isNotNull();
            assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
            assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

            dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                assertThat(paymentDto.getReference()).isNotNull();
                assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                paymentReference.set(paymentDto.getReference());

            });

        });

        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
                = PaymentFixture.bouncedChequeRequest(paymentReference.get());

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusBouncedChequeDto);

        PaymentFailureResponse paymentsFailureResponse =
                paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getPaymentReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusBouncedChequeDto.getFailureReference());
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate(actualDateTime.plusMinutes(15).toString())
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusBouncedChequeDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusSecond_card() {

        // Create a Card payment
        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                .s2sToken(SERVICE_TOKEN)
                .returnUrl("https://www.moneyclaims.service.gov.uk")
                .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
                .then().created(savedPayment -> {
            paymentReference.set(savedPayment.getReference());

            assertNotNull(savedPayment.getReference());
        });

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
                = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
                paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate(actualDateTime.plusMinutes(15).toString())
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_paymentStatusSecond_telephony() {

        // Create a Telephony payment
        String ccdCaseNumber = "1111221233124412";
        FeeDto feeDto = FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("550.00"))
                .ccdCaseNumber(ccdCaseNumber)
                .version("4")
                .code("FEE0002")
                .description("Application for a third party debt order")
                .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
                .amount(new BigDecimal("550"))
                .ccdCaseNumber(ccdCaseNumber)
                .currency(CurrencyCode.GBP)
                .caseType("DIVORCE")
                .returnURL("https://www.moneyclaims.service.gov.uk")
                .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

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
                assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                paymentReference.set(telephonyCardPaymentsResponse.getPaymentReference());
            });
            // pci-pal callback
            TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

            dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

        });

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
                = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
                paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                        .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate(actualDateTime.plusMinutes(15).toString())
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return404_paymentStatusSecond_when_failure_not_found() {

        // Ping 2
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.Yes)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, "abcdefgh",
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), NOT_FOUND.value());
        assertEquals("No Payment Failure available for the given Failure reference", ping2Response.getBody().prettyPrint());
    }

    @Test
    public void negative_return400_paymentStatusSecond_when_invalid_format() {

        // Ping 2
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(null)
                .representmentDate(null)
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, "string",
                paymentStatusUpdateSecond);

        assertEquals(BAD_REQUEST.value(), ping2Response.getStatusCode());
    }

    @Test
    public void negative_return404_chargeback_payment_failure_when_dispute_amount_is_more_than_payment_amount() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("49.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(chargebackResponse.getBody().prettyPrint()).isEqualTo(
                "Failure amount is more than the possible amount");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return404_bounce_cheque_payment_failure_when_dispute_amount_is_more_than_payment_amount() {

        String ccdCaseNumber = "1111221233124419";
        String dcn = "3456908723459910" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("110.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("110.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });
            PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
                = PaymentFixture.bouncedChequeRequest(paymentReference.get());
            Response bounceChequeResponse = paymentTestService.postBounceCheque(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
                "Dispute amount can not be less than payment amount");

        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
        }

    @Test
    public void positive_chargeback_payment_failure_for_card_payment() {

        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
            .then().created(savedPayment -> {
                paymentReference.set(savedPayment.getReference());

                assertNotNull(savedPayment.getReference());
            });

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_bounce_cheque_payment_failure() {

            Random rand = new Random();
            String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
                "111122%04d%04d%02d",
                rand.nextInt(10000),
                rand.nextInt(10000),
                rand.nextInt(99));

            String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
            String dcn = "3456908723459" + RandomUtils.nextInt();

            BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(new BigDecimal("100.00"))
                .service("DIVORCE")
                .siteId("AA01")
                .currency(CurrencyCode.GBP)
                .documentControlNumber(dcn)
                .ccdCaseNumber(ccdCaseNumber)
                .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
                .payerName("CCD User1")
                .bankedDate(DateTime.now().toString())
                .paymentMethod(PaymentMethodType.CHEQUE)
                .paymentStatus(PaymentStatus.SUCCESS)
                .giroSlipNo("GH716376")
                .build();

            PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Collections.singletonList(FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal("100.00"))
                    .code("FEE3132")
                    .version("1")
                    .reference("testRef1")
                    .volume(2)
                    .ccdCaseNumber(ccdCaseNumber1)
                    .build())).build();

            AtomicReference<String> paymentReference = new AtomicReference<>();

            dsl.given().userToken(USER_TOKEN)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(paymentGroupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                    assertThat(paymentGroupFeeDto).isNotNull();
                    assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                    assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                    dsl.given().userToken(USER_TOKEN)
                        .s2sToken(SERVICE_TOKEN)
                        .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                        .then().gotCreated(PaymentDto.class, paymentDto -> {
                            assertThat(paymentDto.getReference()).isNotNull();
                            assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                            assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                            paymentReference.set(paymentDto.getReference());

                        });

                });

            PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
                = PaymentFixture.bouncedChequeRequest(paymentReference.get());

            Response bounceChequeResponse = paymentTestService.postBounceCheque(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusBouncedChequeDto);

            PaymentFailureResponse paymentsFailureResponse =
                paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getPaymentReference()).then()
                    .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

            assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusBouncedChequeDto.getFailureReference());
            assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

            //delete Payment Failure record
            paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_chargeback_payment_failure_for_telephony_payment() {

        String ccdCaseNumber = "1111221233124412";
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

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
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
                PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return404_bounce_cheque_payment_failure_when_cheque_payment_not_found() {

        Random rand = new Random();
        String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
            "111122%04d%04d%02d",
            rand.nextInt(10000),
            rand.nextInt(10000),
            rand.nextInt(99));

        String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
        String dcn = "3456908723459" + RandomUtils.nextInt();

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber1)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentReference.get());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given Payment reference");

    }

    @Test
    public void negative_return429_bounce_cheque_payment_failure_when_duplicate_event_for_cheque_payment() {

        Random rand = new Random();
        String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
            "111122%04d%04d%02d",
            rand.nextInt(10000),
            rand.nextInt(10000),
            rand.nextInt(99));

        String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
        String dcn = "3456908723459909" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("100.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber1)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentReference.get());

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDtoNext
            = PaymentFixture.bouncedChequeRequestForFailureRef(paymentReference.get(), paymentStatusBouncedChequeDto.getFailureReference());
        Response bounceChequeResponseNext = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDtoNext);
        assertThat(bounceChequeResponseNext.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(bounceChequeResponseNext.getBody().prettyPrint()).isEqualTo(
                "Request already received for this failure reference");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
    }


    @Test
    public void negative_return404_chargeback_payment_failure_when_card_payment_not_found() {

        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
            .then().created(savedPayment -> {
                paymentReference.set(savedPayment.getReference());

                assertNotNull(savedPayment.getReference());
            });
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(chargebackResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given Payment reference");

    }

    @Test
    public void negative_return429_chargeback_payment_failure_when_duplicate_event_for_card_payment() {

        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
            .then().created(savedPayment -> {
                paymentReference.set(savedPayment.getReference());

                assertNotNull(savedPayment.getReference());
            });
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentStatusChargebackDto paymentStatusChargebackDtoNext
            = PaymentFixture.chargebackRequestForFailureRef(paymentReference.get(), paymentStatusChargebackDto.getFailureReference());
        Response chargebackResponseNext = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDtoNext);
        assertThat(chargebackResponseNext.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(chargebackResponseNext.getBody().prettyPrint()).isEqualTo(
                "Request already received for this failure reference");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return404_chargeback_payment_failure_when_telephony_payment_not_found() {

        String ccdCaseNumber = "1111221233124412";
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

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
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(chargebackResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given Payment reference");

    }

    @Test
    public void negative_return429_chargeback_payment_failure_when_duplicate_event_for_telephony_payment() {

        String ccdCaseNumber = "1111221233124412";
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

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
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentStatusChargebackDto paymentStatusChargebackDtoNext
            = PaymentFixture.chargebackRequestForFailureRef(paymentReference.get(), paymentStatusChargebackDto.getFailureReference());
        Response chargebackResponseNext = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDtoNext);
        assertThat(chargebackResponseNext.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(chargebackResponseNext.getBody().prettyPrint())
                .isEqualTo("Request already received for this failure reference");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String dcn = "3456908723459907" + RandomUtils.nextInt();
        String failureReference = "FR-123-456" + RandomUtils.nextInt();
        String ccdCaseNumber = "11111244" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        dcn=  dcn.substring(0,21);
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                .amount(new BigDecimal("555"))
                .bankGiroCreditSlipNumber(Integer.valueOf("5"))
                .bankedDate("2022-01-01")
                .currency("GBP")
                .dcnReference(dcn)
                .method("Cash")
                .build();
        paymentTestService.createBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
                .ccdCaseNumber(ccdCaseNumber)
                .documentControlNumbers(new String[]{dcn})
                .isExceptionRecord(false)
                .responsibleServiceId("AA07")
                .build();
        paymentTestService.completeBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(55))
                .failureReference(failureReference)
                .eventDateTime(actualDateTime.plusMinutes(5).toString())
                .reason("RR001")
                .dcn(dcn)
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate(actualDateTime.plusMinutes(20).toString())
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, unprocessedPayment.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
                .statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference())
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return404_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String dcn = "3456908723459902" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11111245" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                .amount(new BigDecimal("555"))
                .bankGiroCreditSlipNumber(Integer.valueOf("5"))
                .bankedDate("2022-01-01")
                .currency("GBP")
                .dcnReference(dcn)
                .method("Cash")
                .build();
        paymentTestService.createBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
                .ccdCaseNumber(ccdCaseNumber)
                .documentControlNumbers(new String[]{dcn})
                .isExceptionRecord(false)
                .responsibleServiceId("AA07")
                .build();
        paymentTestService.completeBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // delete payment record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
                .statusCode(NO_CONTENT.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(100))
                .failureReference("FR111")
                .eventDateTime(actualDateTime.plusMinutes(30).toString())
                .reason("RR001")
                .dcn(dcn)
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given document reference number");
    }

    @Test
    public void negative_return429_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String dcn = "3456908723459903" + RandomUtils.nextInt();
        String failureReference = "FR-123-456" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11112235" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                .amount(new BigDecimal("999"))
                .bankGiroCreditSlipNumber(Integer.valueOf("5"))
                .bankedDate("2022-01-01")
                .currency("GBP")
                .dcnReference(dcn)
                .method("Cash")
                .build();
        paymentTestService.createBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
                .ccdCaseNumber(ccdCaseNumber)
                .documentControlNumbers(new String[]{dcn})
                .isExceptionRecord(false)
                .responsibleServiceId("AA07")
                .build();
        paymentTestService.completeBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference(failureReference)
                .eventDateTime(actualDateTime.plusMinutes(30).toString())
                .reason("RR001")
                .dcn(dcn)
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 1 for Unprocessed Payment event again
        Response bounceChequeResponse1 = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse1.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(bounceChequeResponse1.getBody().prettyPrint()).isEqualTo(
                "Request already received for this failure reference");

        // delete payment record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
                .statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference())
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return400_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String dcn = "3456908723459904" + RandomUtils.nextInt();
        String failureReference = "FR-123-456" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11117235" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                .amount(new BigDecimal("555"))
                .bankGiroCreditSlipNumber(Integer.valueOf("5"))
                .bankedDate("2022-01-01")
                .currency("GBP")
                .dcnReference(dcn)
                .method("Cash")
                .build();
        paymentTestService.createBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
                .ccdCaseNumber(ccdCaseNumber)
                .documentControlNumbers(new String[]{dcn})
                .isExceptionRecord(false)
                .responsibleServiceId("AA07")
                .build();
        paymentTestService.completeBulkScanPayment(
                SERVICE_TOKEN_PAYMENT,
                bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference(failureReference)
                .eventDateTime(actualDateTime.plusHours(2).toString())
                .reason("RR001")
                .dcn(dcn)
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
                .statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_unprocessedPayment_update_payment() {

        String dcn = "3456908723459905" + RandomUtils.nextInt();
        String failureReference = "FR-123-456" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11118235" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .amount(new BigDecimal("555"))
            .bankGiroCreditSlipNumber(Integer.valueOf("5"))
            .bankedDate("2022-01-01")
            .currency("GBP")
            .dcnReference(dcn)
            .method("cheque")
            .build();
        paymentTestService.createBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(new String[]{dcn})
            .isExceptionRecord(false)
            .responsibleServiceId("AA07")
            .build();
        paymentTestService.completeBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
            .amount(BigDecimal.valueOf(55))
            .failureReference(failureReference)
            .eventDateTime(actualDateTime.plusMinutes(30).toString())
            .reason("RR001")
            .dcn(dcn)
            .poBoxNumber("8")
            .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
            SERVICE_TOKEN_PAYMENT,
            unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("555"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("5")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });

        dsl.given()
            .s2sToken(SERVICE_TOKEN)
            .when().unprocessedPaymentUpdateJob()
            .then()
            .ok();

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference.get()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getPaymentReference()).isEqualTo(paymentReference.get());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference()).then().statusCode(NO_CONTENT.value());

        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
            .statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_unprocessedPayment_update_payment_after_second_ping() {

        // Create a Bulk scan payment
        String dcn = "3456908723459906" + RandomUtils.nextInt();
        String failureReference = "FR-123-456" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11114335" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .amount(new BigDecimal("555"))
            .bankGiroCreditSlipNumber(Integer.valueOf("5"))
            .bankedDate("2022-01-01")
            .currency("GBP")
            .dcnReference(dcn)
            .method("cheque")
            .build();
        paymentTestService.createBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(new String[]{dcn})
            .isExceptionRecord(false)
            .responsibleServiceId("AA07")
            .build();
        paymentTestService.completeBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
            .amount(BigDecimal.valueOf(55))
            .failureReference(failureReference)
            .eventDateTime(actualDateTime.plusMinutes(30).toString())
            .reason("RR001")
            .dcn(dcn)
            .poBoxNumber("8")
            .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
            SERVICE_TOKEN_PAYMENT,
            unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });

        // Ping 2 for Unprocessed Payment event
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.No)
            .representmentDate(actualDateTime.plusMinutes(40).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN_PAYMENT, unprocessedPayment.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        dsl.given()
            .s2sToken(SERVICE_TOKEN)
            .when().unprocessedPaymentUpdateJob()
            .then()
            .ok();

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference.get()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureClosed().getPaymentReference()).isEqualTo(paymentReference.get());


        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference()).then().statusCode(NO_CONTENT.value());

        // delete bulk scan record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
            .statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusReport() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("125.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        // Get pba payment by reference
        PaymentDto paymentsResponse =
            paymentTestService.getPayments(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then()
                .statusCode(OK.value()).extract().as(PaymentDto.class);

        // issue a refund
        String paymentReference = paymentsResponse.getPaymentReference();
        int paymentId = paymentsResponse.getFees().get(0).getId();

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "5");
        PaymentRefundRequest paymentRefundRequest
            = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "125", "125");
        Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            paymentRefundRequest);
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        String representmentDate = actualDateTime.plusMinutes(10).toString();

        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(representmentDate)
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis())));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis())));
        Response response = RestAssured.given()
            .header("Authorization", USER_TOKEN_PAYMENT)
            .header("ServiceAuthorization", SERVICE_TOKEN_PAYMENT)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/payment-failures/failure-report");

        PaymentFailureReportResponse paymentFailureReportResponse = response.getBody().as(PaymentFailureReportResponse.class);

        PaymentFailureReportDto paymentFailureReportDto =  paymentFailureReportResponse.getPaymentFailureReportList().stream().filter(s->s.getFailureReference().equalsIgnoreCase(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference())).findFirst().get();
        String eventDate = getReportDate(paymentFailureReportDto.getEventDate());
        String representmentReportDate = getReportDate(paymentFailureReportDto.getRepresentmentDate());
        String expectedDate = getReportDate(new Date(System.currentTimeMillis()));
        assertEquals(paymentDto.getReference(),paymentFailureReportDto.getPaymentReference());
        assertEquals("Chargeback",paymentFailureReportDto.getEventName());
        assertEquals(new BigDecimal("50.00"),paymentFailureReportDto.getDisputedAmount());
        assertEquals(paymentStatusChargebackDto.getFailureReference(),paymentFailureReportDto.getFailureReference());
        assertEquals("Yes",paymentFailureReportDto.getRepresentmentStatus());
        assertEquals("ABA6",paymentFailureReportDto.getOrgId());
        assertEquals(accountPaymentRequest.getCcdCaseNumber(),paymentFailureReportDto.getCcdReference());
        assertEquals("Probate",paymentFailureReportDto.getServiceName());
        assertEquals("125.00", paymentFailureReportDto.getRefundAmount());
        assertEquals(expectedDate,eventDate);
        assertEquals(expectedDate,representmentReportDate);
        assertEquals(refundResponseFromPost.getRefundReference(),paymentFailureReportDto.getRefundReference());
        assertEquals("RR001",paymentFailureReportDto.getFailureReason());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusReport_no_refund_representment() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("125.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis())));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis())));
        Response response = RestAssured.given()
            .header("Authorization", USER_TOKEN_PAYMENT)
            .header("ServiceAuthorization", SERVICE_TOKEN_PAYMENT)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/payment-failures/failure-report");

        PaymentFailureReportResponse paymentFailureReportResponse = response.getBody().as(PaymentFailureReportResponse.class);

        PaymentFailureReportDto paymentFailureReportDto =  paymentFailureReportResponse.getPaymentFailureReportList().stream().filter(s->s.getFailureReference().equalsIgnoreCase(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference())).findFirst().get();
        String eventDate = getReportDate(paymentFailureReportDto.getEventDate());
        String expectedDate = getReportDate(new Date(System.currentTimeMillis()));
        assertEquals(paymentDto.getReference(),paymentFailureReportDto.getPaymentReference());
        assertEquals("Chargeback",paymentFailureReportDto.getEventName());
        assertEquals(new BigDecimal("50.00"),paymentFailureReportDto.getDisputedAmount());
        assertEquals(paymentStatusChargebackDto.getFailureReference(),paymentFailureReportDto.getFailureReference());
        assertEquals("ABA6",paymentFailureReportDto.getOrgId());
        assertEquals(accountPaymentRequest.getCcdCaseNumber(),paymentFailureReportDto.getCcdReference());
        assertEquals("Probate",paymentFailureReportDto.getServiceName());
        assertEquals(expectedDate,eventDate);
        assertEquals("RR001",paymentFailureReportDto.getFailureReason());
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusReport_multiple_refund() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbateSinglePaymentFor2Fees("640.00",
                "PROBATE", "PBAFUNC12345",
                "FEE0001","90.00","FEE002","550.00");
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
        final Integer feeId1 = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(1).getId();
        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId)
            .then().getResponse();

        Response response1 = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("5.00"), paymentGroupReference, feeId1)
            .then().getResponse();

        String remissionReference = response.getBody().jsonPath().getString("remission_reference");
        String remissionReference1 = response1.getBody().jsonPath().getString("remission_reference");

        String ccdCaseNumber = accountPaymentRequest.getCcdCaseNumber();
        paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
            ccdCaseNumber, "20");

        Response refundResponse = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetrospectiveRemissionRequest.retrospectiveRemissionRequestWith().remissionReference(remissionReference)
            .contactDetails(ContactDetails.contactDetailsWith().
                addressLine("High Street 112")
                .country("UK")
                .county("Londonshire")
                .city("London")
                .postalCode("P1 1PO")
                .email("person@gmail.com")
                .notificationType("EMAIL")
                .build())
            .build());

        Response refundResponse1 = paymentTestService.postSubmitRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
            SERVICE_TOKEN_PAYMENT,
            RetrospectiveRemissionRequest.retrospectiveRemissionRequestWith().remissionReference(remissionReference1)
                .contactDetails(ContactDetails.contactDetailsWith().
                    addressLine("High Street 112")
                    .country("UK")
                    .county("Londonshire")
                    .city("London")
                    .postalCode("P1 1PO")
                    .email("person@gmail.com")
                    .notificationType("EMAIL")
                    .build())
                .build());

        assertThat(refundResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(refundResponse1.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
        RefundResponse refundResponseFromPost1 = refundResponse1.getBody().as(RefundResponse.class);
        assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isTrue();
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentDto.getReference());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2

        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        String representmentDate = actualDateTime.plusMinutes(10).toString();

        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(representmentDate)
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis())));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis())));
        Response responseReport = RestAssured.given()
            .header("Authorization", USER_TOKEN_PAYMENT)
            .header("ServiceAuthorization", SERVICE_TOKEN_PAYMENT)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/payment-failures/failure-report");

        PaymentFailureReportResponse paymentFailureReportResponse = responseReport.getBody().as(PaymentFailureReportResponse.class);
        String joinedRefundReference = String.join(",", refundResponseFromPost.getRefundReference(), refundResponseFromPost1.getRefundReference());

        String firstRefundAmount = String.valueOf(refundResponseFromPost.getRefundAmount().toString());
        String secondRefundAmount = String.valueOf(refundResponseFromPost1.getRefundAmount().toString());
        String joinedRefundAmount = String.join(",", firstRefundAmount, secondRefundAmount);
        PaymentFailureReportDto paymentFailureReportDto =  paymentFailureReportResponse.getPaymentFailureReportList().stream().filter(s->s.getFailureReference().equalsIgnoreCase(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference())).findFirst().get();
        String eventDate = getReportDate(paymentFailureReportDto.getEventDate());
        String representmentReportDate = getReportDate(paymentFailureReportDto.getRepresentmentDate());
        String expectedDate = getReportDate(new Date(System.currentTimeMillis()));
        assertEquals(paymentDto.getReference(),paymentFailureReportDto.getPaymentReference());
        assertEquals("Chargeback",paymentFailureReportDto.getEventName());
        assertEquals(new BigDecimal("50.00"),paymentFailureReportDto.getDisputedAmount());
        assertEquals(paymentStatusChargebackDto.getFailureReference(),paymentFailureReportDto.getFailureReference());
        assertEquals("Yes",paymentFailureReportDto.getRepresentmentStatus());
        assertEquals("ABA6",paymentFailureReportDto.getOrgId());
        assertEquals(accountPaymentRequest.getCcdCaseNumber(),paymentFailureReportDto.getCcdReference());
        assertEquals("Probate",paymentFailureReportDto.getServiceName());
        assertThat(joinedRefundAmount.contains(paymentFailureReportDto.getRefundAmount()));
        assertEquals(expectedDate,eventDate);
        assertEquals(expectedDate,representmentReportDate);
        assertThat(joinedRefundReference.contains(paymentFailureReportDto.getRefundReference()));
        assertEquals("RR001",paymentFailureReportDto.getFailureReason());


        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return400_bounce_cheque_payment_is_not_cheque() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "3456908723459911" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CASH)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });

        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequest(paymentReference.get());
        LOG.info("negative_return400_bounce_cheque_payment_is_not_cheque");
        LOG.info("Payment Reference {}", paymentStatusBouncedChequeDto.getPaymentReference());
        LOG.info("Failure reference {}", paymentStatusBouncedChequeDto.getFailureReference());
        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);
        LOG.info("Bounce cheque Payment Reference {}", bounceChequeResponse.getBody());

         assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
            "Incorrect payment method");
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return400_bounce_cheque_payment_event_date_less_than_banked_date() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "3456908723459908" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("450.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });

        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequestForLessEventTime(paymentReference.get());

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
            "Failure event date can not be prior to banked date");
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void positive_paymentStatusSecond_card_update_has_amount_debited_No() {

        // Create a Card payment
        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
            .then().created(savedPayment -> {
                paymentReference.set(savedPayment.getReference());

                assertNotNull(savedPayment.getReference());
            });

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getHasAmountDebited()).isEqualTo("Yes");

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);
        PaymentFailureResponse paymentsFailureResponse1 =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);
        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());
        assertThat(paymentsFailureResponse1.getPaymentFailureList().get(0).getPaymentFailureInitiated().getHasAmountDebited()).isEqualTo("No");

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return400_chargeback_payment_event_date_less_than_payment_date() {

        String ccdCaseNumber = "1111221233124412";
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

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
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestForLessEventTime(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        assertThat(chargebackResponse.getBody().prettyPrint()).isEqualTo(
            "Failure event date can not be prior to payment date");
        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

    }

    @Test
    public void negative_return400_unprocessedPayment_bulk_scan_event_date_less_than_banked_date() {

        // Create a Bulk scan payment
        String dcn = "3456908723459919" + RandomUtils.nextInt();
        dcn=  dcn.substring(0,21);
        String ccdCaseNumber = "11671235" + RandomUtils.nextInt();
        if(ccdCaseNumber.length()>16){
            ccdCaseNumber = ccdCaseNumber.substring(0,16);
        }
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .amount(new BigDecimal("555"))
            .bankGiroCreditSlipNumber(Integer.valueOf("5"))
            .bankedDate("2022-01-01")
            .currency("GBP")
            .dcnReference(dcn)
            .method("Cash")
            .build();
        paymentTestService.createBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayment, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Complete a Bulk scan payment
        BulkScanPayments bulkScanPayments = BulkScanPayments.createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(new String[]{dcn})
            .isExceptionRecord(false)
            .responsibleServiceId("AA07")
            .build();
        paymentTestService.completeBulkScanPayment(
            SERVICE_TOKEN_PAYMENT,
            bulkScanPayments, testProps.bulkScanUrl).then().statusCode(CREATED.value());

        // Ping 1 for Unprocessed Payment event
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
            .amount(BigDecimal.valueOf(55))
            .failureReference("FR3333")
            .eventDateTime(actualDateTime.minusHours(5).toString())
            .reason("RR001")
            .dcn(dcn)
            .poBoxNumber("8")
            .build();

        Response unprocessedPaymentResponse = paymentTestService.postUnprocessedPayment(
            SERVICE_TOKEN_PAYMENT,
            unprocessedPayment);
        assertThat(unprocessedPaymentResponse.getBody().prettyPrint()).isEqualTo(
            "Failure event date can not be prior to banked date");
        assertThat(unprocessedPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // delete payment record
        paymentTestService.deleteBulkScanPayment(SERVICE_TOKEN, dcn, testProps.bulkScanUrl).then()
            .statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return400_paymentStatusSecond_representment_date_less_than_event_date() {

        // Create a Card payment
        AtomicReference<String> paymentReference = new AtomicReference<>();
        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(PaymentFixture.cardPaymentRequestAdoption("215.55", "ADOPTION"))
            .then().created(savedPayment -> {
                paymentReference.set(savedPayment.getReference());

                assertNotNull(savedPayment.getReference());
            });

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequest(paymentReference.get());

        Response chargebackResponse = paymentTestService.postChargeback(
            SERVICE_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getHasAmountDebited()).isEqualTo("Yes");

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(actualDateTime.minusHours(1).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);
        assertThat(ping2Response.getBody().prettyPrint()).isEqualTo(
            "Representment date can not be prior to failure event date");
        assertThat(ping2Response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

    }

    private String getReportDate(Date date) {
        java.time.format.DateTimeFormatter reportNameDateFormat = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return date == null ? null : java.time.LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(reportNameDateFormat);
    }

    private static RetroRemissionRequest getRetroRemissionRequest(final String remissionAmount) {
        return RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfAmount(new BigDecimal(remissionAmount))
            .hwfReference("HWF-A1B-23C")
            .build();
    }

}

