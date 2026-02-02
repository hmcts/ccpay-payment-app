package uk.gov.hmcts.payment.functional;

import com.mifmif.common.regex.Generex;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
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
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.TelephonyCallbackDto;
import uk.gov.hmcts.payment.api.dto.TelephonyPaymentsReportDto;
import uk.gov.hmcts.payment.api.dto.TelephonyPaymentsReportResponse;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class TelephonyPaymentsTest {
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static String USER_TOKEN;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
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
    private static final Logger LOG = LoggerFactory.getLogger(TelephonyPaymentsTest.class);

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("citizen");
            USER_TOKEN = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());
            User user2 = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createTelephonyPaymentAndExpectSuccess() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertEquals("payment status is properly set", "Success", paymentDto.getStatus());
            });
    }

    @Test
    public void retrieveASuccessfulTelephonyPaymentViaLookup() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);
        String status = "success";

        String startDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertEquals("payment status is properly set", "Success", paymentDto.getStatus());
                //update the status
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().updatePaymentStatus(paymentReference, status)
                    .then().noContent();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }
                String endDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when()
                    .enableSearch()
                    .searchPaymentsByServiceBetweenDates("Civil Money Claims", startDateTime, endDateTime)
                    .then().got(PaymentsResponse.class, paymentsResponse -> {
                        assertTrue("correct payment has been retrieved",
                            paymentsResponse.getPayments().stream()
                                .anyMatch(o -> o.getPaymentReference().equals(paymentReference)));
                        PaymentDto paymentRetrieved = paymentsResponse.getPayments().stream().filter(o -> o.getPaymentReference().equals(paymentReference)).findFirst().get();
                        assertEquals("correct payment reference retrieved", paymentRetrieved.getCaseReference(), paymentRecordRequest.getReference());
                        assertEquals("payment status is properly set", "success", paymentRetrieved.getStatus());
                    });
            });
    }

    @Test
    public void retrieveAFailedTelephonyPaymentViaLookup() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);
        String status = "failed";

        String startDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertEquals("payment status is properly set", "Success", paymentDto.getStatus());
                //update the status
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().updatePaymentStatus(paymentReference, status)
                    .then().noContent();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }
                String endDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when()
                    .enableSearch()
                    .searchPaymentsByServiceBetweenDates("Civil Money Claims", startDateTime, endDateTime)
                    .then().got(PaymentsResponse.class, paymentsResponse -> {
                        assertTrue("correct payment has been retrieved",
                            paymentsResponse.getPayments().stream()
                                .anyMatch(o -> o.getPaymentReference().equals(paymentReference)));
                        PaymentDto paymentRetrieved = paymentsResponse.getPayments().stream().filter(o -> o.getPaymentReference().equals(paymentReference)).findFirst().get();

                        assertEquals("correct payment reference retrieved", paymentRetrieved.getCaseReference(), paymentRecordRequest.getReference());
                        assertEquals("payment status is properly set", "failed", paymentRetrieved.getStatus());
                    });
            });
    }

    @Test
    public void retrieveAnErrorneousTelephonyPaymentViaLookup() {
        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        PaymentRecordRequest paymentRecordRequest = getTelephonyPayment(telRefNumber);
        String status = "error";

        String startDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createTelephonyPayment(paymentRecordRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertEquals("payment status is properly set", "Success", paymentDto.getStatus());
                //update the status
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().updatePaymentStatus(paymentReference, status)
                    .then().noContent();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }

                String endDateTime = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when()
                    .enableSearch()
                    .searchPaymentsByServiceBetweenDates("Civil Money Claims", startDateTime, endDateTime)
                    .then().got(PaymentsResponse.class, paymentsResponse -> {
                        assertTrue("correct payment has been retrieved",
                            paymentsResponse.getPayments().stream()
                                .anyMatch(o -> o.getPaymentReference().equals(paymentReference)));
                        PaymentDto paymentRetrieved = paymentsResponse.getPayments().stream().filter(o -> o.getPaymentReference().equals(paymentReference)).findFirst().get();
                        assertEquals("correct payment reference retrieved", paymentRetrieved.getCaseReference(), paymentRecordRequest.getReference());
                        assertEquals("payment status is properly set", "failed", paymentRetrieved.getStatus());
                    });
            });
    }

    @Test
    public void createASuccessfulCardPaymentWithChannelTelephonyAndProviderPciPal() {
        String ccdCaseNumber = "11118888" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        String telRefNumber = new Generex("TEL_PAY_\\d{8}").random();
        CardPaymentRequest paymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("100"))
            .description("description")
            .caseReference(telRefNumber)
            .ccdCaseNumber(ccdCaseNumber)
            .service("PROBATE")
            .currency(CurrencyCode.GBP)
            .provider("pci pal")
            .channel("telephony")
            .siteId("siteId")
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .code("feeCode")
                .version("1")
                .calculatedAmount(new BigDecimal("100.1"))
                .build()))
            .build();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(paymentRequest)
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertTrue(paymentDto.getReference().matches(PAYMENT_REFERENCE_REGEX));
                assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
            });
    }

    @Test
    public void retrieveCorrectPciPalUrlWhenCreatingATelephonyCardPayment() {
        String ccdCaseNumber = "11118888" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        TelephonyCardPaymentsRequest paymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("99.99"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("LegacySearch")
            .telephonySystem("Kerv")
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(getPaymentFeeGroupRequest())
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(getPaymentFeeGroupRequest());

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(paymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                        assertTrue(telephonyCardPaymentsResponse.getPaymentReference().matches(PAYMENT_REFERENCE_REGEX));
                        assertEquals("payment status is properly set", "Initiated", telephonyCardPaymentsResponse.getStatus());
                        String[] schemes = {"https"};
                        UrlValidator urlValidator = new UrlValidator(schemes);
                        assertNotNull(telephonyCardPaymentsResponse.getLinks().getNextUrl());
                        assertTrue(urlValidator.isValid(telephonyCardPaymentsResponse.getLinks().getNextUrl().getHref()));
                    });

            });
    }

    @Test
    public void telephonyPaymentReportValidation() {
        String ccdCaseNumber = "11118888" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("593.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("6")
            .code("FEE0002")
            .description("Filing an application for a divorce, nullity or civil partnership dissolution")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("593"))
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
                    .orderAmount("593")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();
            });

        String dateFrom = paymentTestService.getReportDate(new Date(System.currentTimeMillis()));
        String dateTo = paymentTestService.getReportDate(new Date(System.currentTimeMillis()));

        Response response = paymentTestService.getTelephonyPaymentsByStartAndEndDate(USER_TOKEN, SERVICE_TOKEN, dateFrom, dateTo)
            .then()
            .statusCode(OK.value()).extract().response();

        TelephonyPaymentsReportResponse telephonyPaymentsReportResponse = response.getBody().as(TelephonyPaymentsReportResponse.class);

        TelephonyPaymentsReportDto telephonyPaymentsReportDto = telephonyPaymentsReportResponse.getTelephonyPaymentsReportList().stream().filter(s -> s.getPaymentReference().equalsIgnoreCase(paymentReference)).findFirst().get();
        String paymentDate = paymentTestService.getReportDate(telephonyPaymentsReportDto.getPaymentDate());
        String expectedDate = paymentTestService.getReportDate(new Date(System.currentTimeMillis()));
        assertEquals(paymentReference, telephonyPaymentsReportDto.getPaymentReference());
        assertEquals("Divorce", telephonyPaymentsReportDto.getServiceName());
        assertEquals(ccdCaseNumber, telephonyPaymentsReportDto.getCcdReference());
        assertEquals("FEE0002", telephonyPaymentsReportDto.getFeeCode());
        assertEquals(expectedDate, paymentDate);
        assertEquals(new BigDecimal("593.00"), telephonyPaymentsReportDto.getAmount());
        assertEquals("success", telephonyPaymentsReportDto.getPaymentStatus());
    }

    private PaymentRecordRequest getTelephonyPayment(String reference) {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .externalProvider("pci pal")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("telephony").build())
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference(reference)
            .service("CMC")
            .currency(CurrencyCode.GBP)
            .externalReference(reference)
            .siteId("AA01")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE012345")
                        .reference("ref_1234")
                        .version("1")
                        .volume(1)
                        .build()
                )
            )
            .reportedDateOffline(DateTime.now().toString())
            .build();
    }

    private PaymentGroupDto getPaymentFeeGroupRequest() {
        return PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250.00"))
                .code("FEE3232")
                .version("1")
                .reference("testRef")
                .volume(2)
                .build())).build();
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
