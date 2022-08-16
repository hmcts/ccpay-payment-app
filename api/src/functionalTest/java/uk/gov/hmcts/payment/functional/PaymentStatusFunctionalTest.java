package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.*;
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
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";

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
        PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest("RR001", paymentDto.getReference());
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
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusChargebackDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("Successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void positive_paymentStatusSecond_bulk_scan() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "34569087234591";

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
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, paymentStatusBouncedChequeDto.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("Successful operation", ping2Response.getBody().prettyPrint());

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
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate("2022-10-10T10:10:10")
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
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate("2022-10-10T10:10:10")
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
                "Failure amount cannot be more than payment amount");
        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return404_bounce_cheque_payment_failure_when_dispute_amount_is_more_than_payment_amount() {

        String accountNumber = testProps.existingAccountNumber;
        CreditAccountPaymentRequest accountPaymentRequest = PaymentFixture
            .aPbaPaymentRequestForProbate("49.00",
                "PROBATE", "PBAFUNC12345");
        accountPaymentRequest.setAccountNumber(accountNumber);
        PaymentDto paymentDto = paymentTestService.postPbaPayment(USER_TOKEN, SERVICE_TOKEN, accountPaymentRequest).then()
            .statusCode(CREATED.value()).body("status", equalTo("Success")).extract().as(PaymentDto.class);
            PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
                = PaymentFixture.bouncedChequeRequest(paymentDto.getReference());
            Response bounceChequeResponse = paymentTestService.postBounceCheque(
                SERVICE_TOKEN_PAYMENT,
                paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
                "Failure amount cannot be more than payment amount");

        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentDto.getReference()).then().statusCode(NO_CONTENT.value());
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
        String ccdCaseNumber = "1111221233124419";
        String dcn = "34569087234591";

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

        // Ping 1 for Unprocessed Payment event
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        // Ping 2
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
                .representmentStatus(RepresentmentStatus.No)
                .representmentDate("2022-10-10T10:10:10")
                .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
                SERVICE_TOKEN_PAYMENT, unprocessedPayment.getFailureReference(),
                paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void negative_return404_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "34569087234591";

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

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // Ping 1 for Unprocessed Payment event
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(100))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("34569087234591")
                .poBoxNumber("8")
                .build();

        Response bounceChequeResponse = paymentTestService.postUnprocessedPayment(
                SERVICE_TOKEN_PAYMENT,
                unprocessedPayment);
        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(bounceChequeResponse.getBody().prettyPrint()).isEqualTo(
                "No Payments available for the given Payment reference");
    }

    @Test
    public void negative_return429_unprocessedPayment_bulk_scan() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111221233124419";
        String dcn = "34569087234591";

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

        // Ping 1 for Unprocessed Payment event
        UnprocessedPayment unprocessedPayment = UnprocessedPayment.unprocessedPayment()
                .amount(BigDecimal.valueOf(888))
                .failureReference("FR8888")
                .eventDateTime("2022-10-10T10:10:10")
                .reason("RR001")
                .dcn("88")
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
        paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        // delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN, SERVICE_TOKEN, unprocessedPayment.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }
}
