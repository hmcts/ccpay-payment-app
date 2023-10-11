package uk.gov.hmcts.payment.functional;


import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Java6Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.functional.config.LaunchDarklyFeature;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentTelephonyLiberataPerformanceTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private PaymentTestService paymentTestService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("payments");
            USER_TOKEN = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void telephonyPaymentLiberataValidation() throws Exception {

        String ccdCaseNumber = "11112662" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        /*TelephonyPaymentRequest telephonyPaymentRequest = TelephonyPaymentRequest.createTelephonyPaymentRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("ref124586")
            .currency(CurrencyCode.GBP)
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .caseType("DIVORCE")
            .channel("telephony")
            .provider("pci pal")
            .build();*/

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        AtomicReference<String> paymentRef = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.hmcts.net")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentRef.set(telephonyCardPaymentsResponse.getPaymentReference());
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentRef.get())
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

                // Get pba payments by ccdCaseNumber
                PaymentsResponse liberataResponseOld = paymentTestService.getPbaPaymentsByCCDCaseNumber(SERVICE_TOKEN, telephonyPaymentRequest.getCcdCaseNumber())
                    .then()
                    .statusCode(OK.value()).extract().as(PaymentsResponse.class);

                PaymentsResponse liberataResponseApproach1 = paymentTestService.getPbaPaymentsByCCDCaseNumberApproach1(SERVICE_TOKEN, telephonyPaymentRequest.getCcdCaseNumber())
                    .then()
                    .statusCode(OK.value()).extract().as(PaymentsResponse.class);

                //Comparing the response size of old and new approach
                Java6Assertions.assertThat(liberataResponseOld.getPayments().size()).isGreaterThanOrEqualTo(1);
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().size()).isGreaterThanOrEqualTo(1);

                //Comparing the response of old and new approach
                Boolean compareResult = new HashSet<>(liberataResponseOld.getPayments()).equals(new HashSet<>(liberataResponseApproach1.getPayments()));
                Java6Assertions.assertThat(compareResult).isEqualTo(true);
                LOG.info("Comparison of old and new api end point response of telephony payment is same");

                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getPaymentReference()).isNotNull();
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getAmount()).isEqualTo(new BigDecimal("550.00"));
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getDateCreated()).isNotNull();
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getDateUpdated()).isNotNull();
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getCurrency()).isNotNull();
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getChannel()).isEqualTo("telephony");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getMethod()).isEqualTo("card");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getExternalProvider()).isEqualTo("pci pal");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getStatus()).isEqualTo("success");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getSiteId()).isEqualTo("ABA1");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getServiceName()).isEqualTo("Divorce");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getApportionedPayment()).isEqualTo("550.00");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getCalculatedAmount()).isEqualTo("550.00");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getMemoLine()).isEqualTo("GOV - App for divorce/nullity of marriage or CP");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getNaturalAccountCode()).isEqualTo("4481102159");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getJurisdiction1()).isEqualTo("family");
                Java6Assertions.assertThat(liberataResponseApproach1.getPayments().get(0).getFees().get(0).getJurisdiction2()).isEqualTo("family court");

            });
    }

    @After
    public void deletePayment() {
        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
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
