package uk.gov.hmcts.payment.functional;


import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.BulkScanPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.config.ValidUser;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class RefundRequestorJourneyBulkscanPaymentFunctionalTest {

    private static final Logger LOG = LoggerFactory.getLogger(RefundRequestorJourneyBulkscanPaymentFunctionalTest.class);

    @Autowired
    private PaymentTestService paymentTestService;

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static String SERVICE_TOKEN_PAYMENT;
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static final Pattern REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");
    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private String refundReference;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;

            ValidUser user2 = idamService.createUserWithSearchScope("payments-refund", "payments-refund-divorce");
            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
        }
    }

    @Test
    public void givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup() {

        String[] paymentMethod = {"CHEQUE", "POSTAL_ORDER", "CASH"};
        String[] lag_time = {"20", "20", "5"};

        for (int i = 0; i < paymentMethod.length; i++) {

            Random rand = new Random();
            String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
                "111122%04d%04d%02d",
                rand.nextInt(10000),
                rand.nextInt(10000),
                rand.nextInt(99));

            String ccdCaseNumber1 = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
            ;
            String dcn = "6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

            BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(new BigDecimal("450.00"))
                .service("DIVORCE")
                .siteId("AA01")
                .currency(CurrencyCode.GBP)
                .documentControlNumber(dcn)
                .ccdCaseNumber(ccdCaseNumber)
                .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
                .payerName("CCD User1")
                .bankedDate(DateTime.now().toString())
                .paymentMethod(PaymentMethodType.valueOf(paymentMethod[i]))
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

            dsl.given().userToken(USER_TOKEN_PAYMENT)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(paymentGroupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                    assertThat(paymentGroupFeeDto).isNotNull();
                    assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                    assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                    dsl.given().userToken(USER_TOKEN_PAYMENT)
                        .s2sToken(SERVICE_TOKEN)
                        .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                        .then().gotCreated(PaymentDto.class, paymentDto -> {
                            paymentReference = paymentDto.getReference();
                            assertThat(paymentDto.getReference()).isNotNull();
                            assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                            assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());
                        });

                });

            paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
                ccdCaseNumber, lag_time[i]);

            // Get pba payment by reference
            PaymentDto paymentsResponse =
                paymentTestService.getPayments(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference).then()
                    .statusCode(OK.value()).extract().as(PaymentDto.class);
            int paymentId = paymentsResponse.getFees().get(0).getId();

            PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "225.00", "450");

            RefundResponse refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                    SERVICE_TOKEN_PAYMENT,
                    paymentRefundRequest).
                then().statusCode(CREATED.value()).extract().as(RefundResponse.class);
            refundReference = refundResponse.getRefundReference();

            assertThat(refundResponse.getRefundAmount()).isEqualTo(new BigDecimal("225.00"));
            assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponse.getRefundReference()).matches()).isEqualTo(true);
        }
    }

    @Test
    public void negative_givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup_under_lag_time() {

        String[] paymentMethod = {"CHEQUE", "POSTAL_ORDER", "CASH"};
        String[] lag_time = {"15", "15", "3"};

        for (int i = 0; i < paymentMethod.length; i++) {

            Random rand = new Random();
            String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
                "111122%04d%04d%02d",
                rand.nextInt(10000),
                rand.nextInt(10000),
                rand.nextInt(99));

            String ccdCaseNumber1 = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
            ;
            String dcn = "6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

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
                .paymentMethod(PaymentMethodType.valueOf(paymentMethod[i]))
                .paymentStatus(PaymentStatus.SUCCESS)
                .giroSlipNo("GH716376")
                .build();

            PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
                .fees(Arrays.asList(FeeDto.feeDtoWith()
                    .calculatedAmount(new BigDecimal("450.00"))
                    .code("FEE3132")
                    .version("1")
                    .reference("testRef1")
                    .volume(1)
                    .ccdCaseNumber(ccdCaseNumber1)
                    .build())).build();

            dsl.given().userToken(USER_TOKEN_PAYMENT)
                .s2sToken(SERVICE_TOKEN)
                .when().addNewFeeAndPaymentGroup(paymentGroupDto)
                .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                    assertThat(paymentGroupFeeDto).isNotNull();
                    assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                    assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                    dsl.given().userToken(USER_TOKEN_PAYMENT)
                        .s2sToken(SERVICE_TOKEN)
                        .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                        .then().gotCreated(PaymentDto.class, paymentDto -> {
                            paymentReference = paymentDto.getReference();
                            assertThat(paymentDto.getReference()).isNotNull();
                            assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                            assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());
                        });

                });

            paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
                ccdCaseNumber, lag_time[i]);

            // Get pba payment by reference
            PaymentDto paymentsResponse =
                paymentTestService.getPayments(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference).then()
                    .statusCode(OK.value()).extract().as(PaymentDto.class);
            int paymentId = paymentsResponse.getFees().get(0).getId();

            // initiate the refund
            PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest(paymentId, "RR001", paymentReference, "100", "450");
            LOG.info("Before calling Refund svc for creating refund (ln 272) {}", paymentRefundRequest.getPaymentReference());
            Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                SERVICE_TOKEN_PAYMENT,
                paymentRefundRequest);
            String s = refundResponse.getBody().asString();
            LOG.info("Refund service response (ln 277) {}", s);
            assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(refundResponse.getBody().asString()).isEqualTo("This payment is not yet eligible for refund");
        }
    }

    @After
    public void deletePayment() {
        if (refundReference != null) {
            //delete refund record
            paymentTestService.deleteRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE, SERVICE_TOKEN, refundReference);
        }

        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
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
