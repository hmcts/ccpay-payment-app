package uk.gov.hmcts.payment.functional;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentRecordFunctionalTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordFunctionalTest.class);

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
        String startDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createTelephonyPayment(getPaymentRecordRequest())
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertNotNull(paymentDto.getReference());

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }

                String endDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);
                // search payment and assert the result
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDates(startDate, endDate)
                    .then().getPayments((paymentsResponse -> {
                        LOG.info("paymentsResponse: {}", paymentsResponse.getPayments().size());
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(1);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
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
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA01")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("550.00"))
                        .code("FEE0333")
                        .memoLine("Bar Cash")
                        .naturalAccountCode("21245654433")
                        .version("1")
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
