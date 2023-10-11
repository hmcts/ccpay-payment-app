package uk.gov.hmcts.payment.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.assertj.core.api.Java6Assertions;
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
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class BarPaymentFunctionalTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Logger LOG = LoggerFactory.getLogger(BarPaymentFunctionalTest.class);
    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;
    private static DateTimeZone zoneUTC = DateTimeZone.UTC;
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

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith("citizen");
            USER_TOKEN = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createPaymentRecordAndValidateSearchResults() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        String startDate = formatter.format(LocalDateTime.now(zoneUTC).minusSeconds(30).toDate());

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createTelephonyPayment(getPaymentRecordRequest())
            .then().created(paymentDto -> {
                assertNotNull(paymentDto.getReference());

                paymentReference = paymentDto.getReference();
                // search payment and assert the result

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String endDate = formatter.format(LocalDateTime.now(zoneUTC).toDate());

                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceNameApproach1(startDate, endDate, "cash")
                    .then().getPayments((paymentsResponse -> {
                        LOG.info("paymentsResponse: {}", paymentsResponse.getPayments().size());
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(1);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
                        assertThat(retrievedPaymentDto.getMethod()).isEqualTo("cash");
                        assertThat(retrievedPaymentDto.getAmount()).isIn(new BigDecimal("1.01"), new BigDecimal("0.10"), new BigDecimal("0.01"), new BigDecimal("100.00"), new BigDecimal("550.00"));
                        assertThat(retrievedPaymentDto.getChannel()).isEqualTo("digital bar");
                        assertThat(retrievedPaymentDto.getStatus()).isEqualTo("success");
                        assertThat(retrievedPaymentDto.getServiceName()).isEqualTo("Digital Bar");
                        assertThat(retrievedPaymentDto.getDateCreated()).isNotNull();
                        assertThat(retrievedPaymentDto.getDateUpdated()).isNotNull();
                        assertThat(retrievedPaymentDto.getCurrency()).isNotNull();
                        assertThat(retrievedPaymentDto.getCaseReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getExternalProvider()).isNotNull();
                        assertThat(retrievedPaymentDto.getSiteId()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentGroupReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getReportedDateOffline()).isNotNull();
                        assertThat(retrievedPaymentDto.getGiroSlipNo()).isEqualTo("12345");
                        FeeDto feeDto = retrievedPaymentDto.getFees().get(0);
                        assertThat(feeDto.getCode()).isEqualTo("FEE0333");
                        assertThat(feeDto.getVersion()).isEqualTo("1");
                        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(feeDto.getReference()).isNotNull();
                        assertThat(feeDto.getReference()).isEqualTo("REF_123");
                        assertThat(feeDto.getVolume()).isEqualTo(1);
                    }));
            });
    }

    private PaymentRecordRequest getPaymentRecordRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .paymentMethod(PaymentMethodType.CASH)
            .reference("REF_123")
            .externalProvider("middle office provider")
            .service("DIGITAL_BAR")
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now(zoneUTC).toString())
            .siteId("AA01")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("550.00"))
                        .code("FEE0333")
                        .version("1")
                        .volume(1)
                        .reference("REF_123")
                        .build()
                )
            )
            .build();
    }

    @Test
    public void createBarPostalOrderPaymentRecordAndValidateSearchResults() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        String startDate = formatter.format(LocalDateTime.now(zoneUTC).minusSeconds(30).toDate());

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createTelephonyPayment(getPaymentRecordRequestForPostalOrder())
            .then().created(paymentDto -> {
                LOG.info(paymentDto.getReference());
                assertNotNull(paymentDto.getReference());
                paymentReference = paymentDto.getReference();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String endDate = formatter.format(LocalDateTime.now(zoneUTC).toDate());

                PaymentsResponse liberataResponseOld = dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceName(startDate, endDate, "postal_order")
                    .then().getPayments();

                PaymentsResponse liberataResponseApproach1 = dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceNameApproach1(startDate, endDate, "postal_order")
                    .then().getPayments();

                //Comparing the response size of old and new approach
                Java6Assertions.assertThat(liberataResponseOld.getPayments().size()).
                    isEqualTo(liberataResponseApproach1.getPayments().size());
                LOG.info("" + liberataResponseApproach1.getPayments().size());
                //Comparing the response of old and new approach
                Boolean compareResult = new HashSet<>(liberataResponseOld.getPayments()).equals(new HashSet<>(liberataResponseApproach1.getPayments()));
                Java6Assertions.assertThat(compareResult).isEqualTo(true);
                LOG.info("Comparison of old and new api end point response BAR Postal Order payment is same");

                // search payment and assert the result
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceNameApproach1(startDate, endDate, "postal_order")
                    .then().getPayments((paymentsResponse -> {
                        LOG.info("paymentsResponse: {}", paymentsResponse.getPayments().size());
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(1);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
                        assertThat(retrievedPaymentDto.getMethod()).isEqualTo("postal order");
                        assertThat(retrievedPaymentDto.getAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(retrievedPaymentDto.getChannel()).isEqualTo("digital bar");
                        assertThat(retrievedPaymentDto.getStatus()).isEqualTo("pending");
                        assertThat(retrievedPaymentDto.getServiceName()).isEqualTo("Digital Bar");
                        assertThat(retrievedPaymentDto.getDateCreated()).isNotNull();
                        assertThat(retrievedPaymentDto.getDateUpdated()).isNotNull();
                        assertThat(retrievedPaymentDto.getCurrency()).isNotNull();
                        assertThat(retrievedPaymentDto.getCaseReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getExternalProvider()).isNotNull();
                        assertThat(retrievedPaymentDto.getSiteId()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentGroupReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getReportedDateOffline()).isNotNull();
                        assertThat(retrievedPaymentDto.getGiroSlipNo()).isEqualTo("312131");
                        FeeDto feeDto = retrievedPaymentDto.getFees().get(0);
                        assertThat(feeDto.getCode()).isEqualTo("FEE0002");
                        assertThat(feeDto.getVersion()).isEqualTo("4");
                        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(feeDto.getReference()).isNotNull();
                        assertThat(feeDto.getReference()).isEqualTo("REF_123");
                        assertThat(feeDto.getMemoLine()).isEqualTo("GOV - App for divorce/nullity of marriage or CP");
                        assertThat(feeDto.getNaturalAccountCode()).isEqualTo("4481102159");
                        assertThat(feeDto.getJurisdiction1()).isEqualTo("family");
                        assertThat(feeDto.getJurisdiction2()).isEqualTo("family court");
                        assertThat(feeDto.getVolume()).isEqualTo(1);
                    }));

            });

    }

    private PaymentRecordRequest getPaymentRecordRequestForPostalOrder() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .paymentMethod(PaymentMethodType.POSTAL_ORDER)
            .reference("REF_123")
            .externalProvider("middle office provider")
            .service("DIGITAL_BAR")
            .currency(CurrencyCode.GBP)
            .giroSlipNo("312131")
            .reportedDateOffline(DateTime.now(zoneUTC).toString())
            .siteId("Y431")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("550.00"))
                        .code("FEE0002")
                        .version("4")
                        .volume(1)
                        .reference("REF_123")
                        .build()
                )
            )
            .build();
    }

    @Test
    public void createBarChequePaymentRecordAndValidateSearchResults() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        String startDate = formatter.format(LocalDateTime.now(zoneUTC).minusSeconds(30).toDate());

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createTelephonyPayment(getPaymentRecordRequestForCheque())
            .then().created(paymentDto -> {
                assertNotNull(paymentDto.getReference());
                paymentReference = paymentDto.getReference();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String endDate = formatter.format(LocalDateTime.now(zoneUTC).toDate());

                // search payment and assert the result
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceNameApproach1(startDate, endDate, "cheque")
                    .then().getPayments((paymentsResponse -> {
                        LOG.info("paymentsResponse: {}", paymentsResponse.getPayments().size());
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(1);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
                        assertThat(retrievedPaymentDto.getMethod()).isEqualTo("cheque");
                        assertThat(retrievedPaymentDto.getAmount()).isIn(new BigDecimal("0.10"), new BigDecimal("0.01"), new BigDecimal("100.00"), new BigDecimal("550.00"));
                        assertThat(retrievedPaymentDto.getChannel()).isIn("digital bar", "bulk scan");
                        // assertThat(retrievedPaymentDto.getStatus()).isEqualTo("pending");
                        assertThat(retrievedPaymentDto.getServiceName()).isEqualTo("Digital Bar");
                        assertThat(retrievedPaymentDto.getDateCreated()).isNotNull();
                        assertThat(retrievedPaymentDto.getDateUpdated()).isNotNull();
                        assertThat(retrievedPaymentDto.getCurrency()).isNotNull();
                        assertThat(retrievedPaymentDto.getCaseReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getExternalProvider()).isNotNull();
                        assertThat(retrievedPaymentDto.getSiteId()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentGroupReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getReportedDateOffline()).isNotNull();
                        assertThat(retrievedPaymentDto.getGiroSlipNo()).isEqualTo("312131");
                        FeeDto feeDto = retrievedPaymentDto.getFees().get(0);
                        assertThat(feeDto.getCode()).isEqualTo("FEE0002");
                        assertThat(feeDto.getVersion()).isEqualTo("4");
                        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(feeDto.getReference()).isNotNull();
                        assertThat(feeDto.getReference()).isEqualTo("REF_123");
                        assertThat(feeDto.getMemoLine()).isEqualTo("GOV - App for divorce/nullity of marriage or CP");
                        assertThat(feeDto.getNaturalAccountCode()).isEqualTo("4481102159");
                        assertThat(feeDto.getJurisdiction1()).isEqualTo("family");
                        assertThat(feeDto.getJurisdiction2()).isEqualTo("family court");
                        assertThat(feeDto.getVolume()).isEqualTo(1);
                    }));
            });

    }

    private PaymentRecordRequest getPaymentRecordRequestForCheque() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .paymentMethod(PaymentMethodType.CHEQUE)
            .reference("REF_123")
            .externalProvider("middle office provider")
            .service("DIGITAL_BAR")
            .currency(CurrencyCode.GBP)
            .giroSlipNo("312131")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("Y431")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("550.00"))
                        .code("FEE0002")
                        .version("4")
                        .volume(1)
                        .reference("REF_123")
                        .build()
                )
            )
            .build();
    }

    @Test
    public void createBarCardPaymentRecordAndValidateSearchResults() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        String startDate = formatter.format(LocalDateTime.now(zoneUTC).minusSeconds(30).toDate());

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createTelephonyPayment(getPaymentRecordRequestForCard())
            .then().created(paymentDto -> {
                assertNotNull(paymentDto.getReference());
                paymentReference = paymentDto.getReference();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String endDate = formatter.format(LocalDateTime.now(zoneUTC).toDate());

                // search payment and assert the result
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDatesPaymentMethodServiceNameApproach1(startDate, endDate, "card")
                    .then().getPayments((paymentsResponse -> {
                        LOG.info("paymentsResponse: {}", paymentsResponse.getPayments().size());
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(1);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
                        assertThat(retrievedPaymentDto.getMethod()).isEqualTo("card");
                        assertThat(retrievedPaymentDto.getAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(retrievedPaymentDto.getChannel()).isEqualTo("digital bar");
                        assertThat(retrievedPaymentDto.getStatus()).isEqualTo("success");
                        assertThat(retrievedPaymentDto.getServiceName()).isEqualTo("Digital Bar");
                        assertThat(retrievedPaymentDto.getDateCreated()).isNotNull();
                        assertThat(retrievedPaymentDto.getDateUpdated()).isNotNull();
                        assertThat(retrievedPaymentDto.getCurrency()).isNotNull();
                        assertThat(retrievedPaymentDto.getCaseReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getExternalProvider()).isNotNull();
                        assertThat(retrievedPaymentDto.getSiteId()).isNotNull();
                        assertThat(retrievedPaymentDto.getPaymentGroupReference()).isNotNull();
                        assertThat(retrievedPaymentDto.getReportedDateOffline()).isNotNull();
                        assertThat(retrievedPaymentDto.getGiroSlipNo()).isNull();
                        FeeDto feeDto = retrievedPaymentDto.getFees().get(0);
                        assertThat(feeDto.getCode()).isEqualTo("FEE0002");
                        assertThat(feeDto.getVersion()).isEqualTo("4");
                        assertThat(feeDto.getCalculatedAmount()).isEqualTo(new BigDecimal("550.00"));
                        assertThat(feeDto.getReference()).isNotNull();
                        assertThat(feeDto.getReference()).isEqualTo("REF_123");
                        assertThat(feeDto.getMemoLine()).isEqualTo("GOV - App for divorce/nullity of marriage or CP");
                        assertThat(feeDto.getNaturalAccountCode()).isEqualTo("4481102159");
                        assertThat(feeDto.getJurisdiction1()).isEqualTo("family");
                        assertThat(feeDto.getJurisdiction2()).isEqualTo("family court");
                        assertThat(feeDto.getVolume()).isEqualTo(1);
                    }));
            });

    }

    private PaymentRecordRequest getPaymentRecordRequestForCard() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("550.00"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference("REF_123")
            .externalProvider("middle office provider")
            .service("DIGITAL_BAR")
            .currency(CurrencyCode.GBP)
            .reportedDateOffline(DateTime.now().toString())
            .siteId("Y431")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("550.00"))
                        .code("FEE0002")
                        .version("4")
                        .volume(1)
                        .reference("REF_123")
                        .build()
                )
            )
            .build();
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
