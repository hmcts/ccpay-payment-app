package uk.gov.hmcts.payment.functional;


import io.restassured.response.Response;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
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
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class RefundRequestorJourneyBulkscanPaymentFunctionalTest {

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

    @Autowired
    private LaunchDarklyFeature featureToggler;

    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static String USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE;
    private static String SERVICE_TOKEN_PAYMENT;
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static final Pattern REFUNDS_REGEX_PATTERN = Pattern.compile("^(RF)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;

            USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE =
                idamService.createUserWithSearchScope(CMC_CASE_WORKER_GROUP, "payments-refund")
                    .getAuthorisationToken();
            SERVICE_TOKEN_PAYMENT = s2sTokenService.getS2sToken("ccpay_bubble", testProps.payBubbleS2SSecret);
        }
    }

    @Test
    public void givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup() throws Exception {

        String[] paymentMethod = {"CHEQUE", "POSTAL_ORDER", "CASH"};
        String[] lag_time = {"20", "20", "5"};

        for (int i = 0; i < paymentMethod.length; i++) {


            Random rand = new Random();
            String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
                "111122%04d%04d%02d",
                rand.nextInt(10000),
                rand.nextInt(10000),
                rand.nextInt(99));

            String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
            String dcn = "3456908723459" + RandomUtils.nextInt();

            BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(new BigDecimal(100.00))
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

            Response rollbackPaymentResponse = paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
                ccdCaseNumber, lag_time[i]);
            System.out.println(rollbackPaymentResponse.getBody().prettyPrint());

            PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest("RR001", paymentReference.get());
            Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                SERVICE_TOKEN_PAYMENT,
                paymentRefundRequest);

            System.out.println(refundResponse.getBody().prettyPrint());
            assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
            RefundResponse refundResponseFromPost = refundResponse.getBody().as(RefundResponse.class);
            assertThat(refundResponseFromPost.getRefundAmount()).isEqualTo(new BigDecimal("100.00"));
            System.out.println(refundResponseFromPost.getRefundReference());
            assertThat(REFUNDS_REGEX_PATTERN.matcher(refundResponseFromPost.getRefundReference()).matches()).isEqualTo(true);

        }

    }

    @Test
    public void negative_givenAFeeInPG_WhenABulkScanPaymentNeedsMappingthenPaymentShouldBeAddedToExistingGroup_under_lag_time() throws Exception {
        String[] paymentMethod = {"CHEQUE", "POSTAL_ORDER", "CASH"};
        String[] lag_time = {"15", "15", "3"};

        for (int i = 0; i < paymentMethod.length; i++) {

            Random rand = new Random();
            String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
                "111122%04d%04d%02d",
                rand.nextInt(10000),
                rand.nextInt(10000),
                rand.nextInt(99));

            String ccdCaseNumber1 = "1111-CC12-" + RandomUtils.nextInt();
            String dcn = "3456908723459" + RandomUtils.nextInt();

            BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
                .amount(new BigDecimal(100.00))
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

            Response rollbackPaymentResponse = paymentTestService.updateThePaymentDateByCcdCaseNumberForCertainHours(USER_TOKEN, SERVICE_TOKEN,
                ccdCaseNumber, lag_time[i]);
            System.out.println(rollbackPaymentResponse.getBody().prettyPrint());

            PaymentRefundRequest paymentRefundRequest
                = PaymentFixture.aRefundRequest("RR001", paymentReference.get());
            Response refundResponse = paymentTestService.postInitiateRefund(USER_TOKEN_PAYMENTS_REFUND_REQUESTOR_ROLE,
                SERVICE_TOKEN_PAYMENT,
                paymentRefundRequest);

            System.out.println(refundResponse.getBody().prettyPrint());
            assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(refundResponse.getBody().asString()).isEqualTo("This payment is not yet eligible for refund");

        }

    }
}
