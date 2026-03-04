package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class TelephonyPaymentFunctionalTest {

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

    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String REMISSION_REFERENCE_REGEX = "^[RM-]{3}(\\w{4}-){3}(\\w{4})";
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void telephonyPaymentLiberataValidation() {
        String ccdCaseNumber = "11116467" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .volume(1)
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .telephonySystem("Kerv")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

                // Get pba payments by ccdCaseNumber
                PaymentsResponse liberataResponse = paymentTestService.getReconciliationDataByCCDCaseNumber(SERVICE_TOKEN, telephonyPaymentRequest.getCcdCaseNumber())
                    .then()
                    .statusCode(OK.value()).extract().as(PaymentsResponse.class);

                assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(1);

                assertThat(liberataResponse.getPayments().get(0).getAmount()).isEqualTo(new BigDecimal("550.00"));
                assertThat(liberataResponse.getPayments().get(0).getDateCreated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getDateUpdated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getCurrency().toString()).isEqualTo("GBP");
                assertThat(liberataResponse.getPayments().get(0).getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
                assertThat(liberataResponse.getPayments().get(0).getPaymentReference()).isEqualTo(paymentReference);
                assertThat(liberataResponse.getPayments().get(0).getChannel()).isEqualTo("telephony");
                assertThat(liberataResponse.getPayments().get(0).getMethod()).isEqualTo("card");
                assertThat(liberataResponse.getPayments().get(0).getExternalProvider()).isEqualTo("pci pal");
                assertThat(liberataResponse.getPayments().get(0).getStatus()).isEqualTo("success");
                assertThat(liberataResponse.getPayments().get(0).getSiteId()).isEqualTo("ABA1");
                assertThat(liberataResponse.getPayments().get(0).getServiceName()).isEqualTo("Divorce");
                assertThat(liberataResponse.getPayments().get(0).getPaymentGroupReference()).isEqualTo(paymentGroupReference);

                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getId()).isEqualTo(paymentGroupFeeDto.getFees().get(0).getId());
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getCode()).isEqualTo(paymentGroupFeeDto.getFees().get(0).getCode());
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getVersion()).isEqualTo(paymentGroupFeeDto.getFees().get(0).getVersion());
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getVolume()).isEqualTo(paymentGroupFeeDto.getFees().get(0).getVolume());
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getCalculatedAmount()).isEqualTo("550.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getMemoLine()).isEqualTo("GOV - App for divorce/nullity of marriage or CP");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getNaturalAccountCode()).isEqualTo("4481102159");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getCcdCaseNumber()).isEqualTo(paymentGroupFeeDto.getFees().get(0).getCcdCaseNumber());
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction1()).isEqualTo("family");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction2()).isEqualTo("family court");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getApportionedPayment()).isEqualTo("550.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getDateReceiptProcessed()).isNotNull();
            });
    }

    @Test
    public void telephonyPaymentAntenna() {
        String ccdCaseNumber = "11116467" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("612.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("8")
            .code("FEE0002")
            .volume(1)
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("612"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .telephonySystem("Kerv")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("612")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

                // Get pba payments by ccdCaseNumber
                PaymentsResponse liberataResponse = paymentTestService.getReconciliationDataByCCDCaseNumber(SERVICE_TOKEN, telephonyPaymentRequest.getCcdCaseNumber())
                    .then()
                    .statusCode(OK.value()).extract().as(PaymentsResponse.class);

                //Comparing the response size of old and new approach
                assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(1);

                assertThat(liberataResponse.getPayments().get(0).getPaymentReference()).isEqualTo(paymentReference);
                assertThat(liberataResponse.getPayments().get(0).getAmount()).isEqualTo(new BigDecimal("612.00"));
                assertThat(liberataResponse.getPayments().get(0).getDateCreated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getDateUpdated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getCurrency().toString()).isEqualTo("GBP");
                assertThat(liberataResponse.getPayments().get(0).getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
                assertThat(liberataResponse.getPayments().get(0).getChannel()).isEqualTo("telephony");
                assertThat(liberataResponse.getPayments().get(0).getMethod()).isEqualTo("card");
                assertThat(liberataResponse.getPayments().get(0).getExternalProvider()).isEqualTo("pci pal");
                assertThat(liberataResponse.getPayments().get(0).getStatus()).isEqualTo("success");
                assertThat(liberataResponse.getPayments().get(0).getSiteId()).isEqualTo("ABA1");
                assertThat(liberataResponse.getPayments().get(0).getServiceName()).isEqualTo("Divorce");
                assertThat(liberataResponse.getPayments().get(0).getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getApportionedPayment()).isEqualTo("612.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getCalculatedAmount()).isEqualTo("612.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getMemoLine()).isEqualTo("RECEIPT OF FEES - Family issue divorce");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getNaturalAccountCode()).isEqualTo("4481102159");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction1()).isEqualTo("family");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction2()).isEqualTo("family court");

            });
    }

    @Test
    public void telephonyPaymentKerv() {
        String ccdCaseNumber = "11116467" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("612.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("8")
            .code("FEE0002")
            .volume(1)
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("612"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .telephonySystem("Kerv")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX)).isTrue();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("612")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

                // Get pba payments by ccdCaseNumber
                PaymentsResponse liberataResponse = paymentTestService.getReconciliationDataByCCDCaseNumber(SERVICE_TOKEN, telephonyPaymentRequest.getCcdCaseNumber())
                    .then()
                    .statusCode(OK.value()).extract().as(PaymentsResponse.class);

                assertThat(liberataResponse.getPayments().size()).isGreaterThanOrEqualTo(1);

                assertThat(liberataResponse.getPayments().get(0).getPaymentReference()).isEqualTo(paymentReference);
                assertThat(liberataResponse.getPayments().get(0).getAmount()).isEqualTo(new BigDecimal("612.00"));
                assertThat(liberataResponse.getPayments().get(0).getDateCreated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getDateUpdated()).isNotNull();
                assertThat(liberataResponse.getPayments().get(0).getCurrency().toString()).isEqualTo("GBP");
                assertThat(liberataResponse.getPayments().get(0).getCcdCaseNumber()).isEqualTo(ccdCaseNumber);
                assertThat(liberataResponse.getPayments().get(0).getChannel()).isEqualTo("telephony");
                assertThat(liberataResponse.getPayments().get(0).getMethod()).isEqualTo("card");
                assertThat(liberataResponse.getPayments().get(0).getExternalProvider()).isEqualTo("pci pal");
                assertThat(liberataResponse.getPayments().get(0).getStatus()).isEqualTo("success");
                assertThat(liberataResponse.getPayments().get(0).getSiteId()).isEqualTo("ABA1");
                assertThat(liberataResponse.getPayments().get(0).getServiceName()).isEqualTo("Divorce");
                assertThat(liberataResponse.getPayments().get(0).getPaymentGroupReference()).isEqualTo(paymentGroupReference);
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getApportionedPayment()).isEqualTo("612.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getCalculatedAmount()).isEqualTo("612.00");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getMemoLine()).isEqualTo("RECEIPT OF FEES - Family issue divorce");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getNaturalAccountCode()).isEqualTo("4481102159");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction1()).isEqualTo("family");
                assertThat(liberataResponse.getPayments().get(0).getFees().get(0).getJurisdiction2()).isEqualTo("family court");

            });
    }

    @Test
    public void telephonyPaymentUnsupportedTelephonySystemConfiguration() {
        String ccdCaseNumber = "11116467" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("612.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("8")
            .code("FEE0002")
            .volume(1)
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("612"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .telephonySystem("JohnDoe")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference).then().getResponse();
                assertThat(response.statusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
                assertThat(response.getBody().print()).isEqualTo("Invalid or missing attributes");
            });
    }

    @Test
    public void telephonyPaymentUnsupportedServiceByFeePay() {
        String ccdCaseNumber = "11116467" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("612.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("8")
            .code("FEE0002")
            .volume(1)
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("612"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("Test")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .telephonySystem("Kerv")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference).then().getResponse();
                assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
                assertThat(response.getBody().print()).isEqualTo("No Service found for given CaseType or HMCTS Org Id");
            });
    }

    @After
    public void deletePayment() {
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
