package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class PaymentsSearchFunctionalTest {

    private static final String DATE_FORMAT_DD_MM_YYYY = "dd-MM-yyyy";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";

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
    private static DateTimeZone zoneUTC = DateTimeZone.UTC;
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

    /*@Test
    public void givenAnyTwoValidDatesWithFormatYYYYMMDDShouldNotBeAnyErrors() {
        String startDate = LocalDate.now(zoneUTC).toString(DATE_FORMAT);
        String endDate = LocalDate.now(zoneUTC).toString(DATE_FORMAT);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().getPayments206(paymentsResponse -> {
                assertThat(paymentsResponse.getPayments()).isNotNull();
        });
    }


    @Test
    public void givenAnyTwoValidDatesWithFormatDDMMYYYYShouldNotBeAnyErrors() {
        String startDate = LocalDate.now(zoneUTC).toString(DATE_FORMAT_DD_MM_YYYY);
        String endDate = LocalDate.now(zoneUTC).toString(DATE_FORMAT_DD_MM_YYYY);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().getPayments206(paymentsResponse -> {
                assertThat(paymentsResponse.getPayments()).isNotNull();
        });
    }*/


    @Test
    public void givenFutureEndDateTheSearchPaymentsShouldFail() {
        String startDate = LocalDateTime.now(zoneUTC).toString(DATE_TIME_FORMAT);
        String endDate = LocalDateTime.now(zoneUTC).plusMinutes(1).toString(DATE_TIME_FORMAT);

        Response response =  dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Date cannot be in the future");
    }

    @Test
    public void givenFutureStartDateTheSearchPaymentsShouldFail() {
        String startDate = LocalDateTime.now(zoneUTC).plusDays(1).toString(DATE_TIME_FORMAT);
        String endDate = LocalDateTime.now(zoneUTC).toString(DATE_TIME_FORMAT);

        Response response =  dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Date cannot be in the future");
    }

    @Test
    public void searchPaymentWithStartDateGreaterThanEndDateShouldFail() throws Exception {
        String startDate = LocalDateTime.now(zoneUTC).plusMinutes(1).toString(DATE_TIME_FORMAT);
        String endDate = LocalDateTime.now(zoneUTC).toString(DATE_TIME_FORMAT);

        Response response =  dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Start date cannot be greater than end date");
    }

    @Test
    public void givenTwoPaymentsInPeriodWhensearchPaymentsWithStartDateEndDateThenShouldPass() throws InterruptedException {
        Thread.sleep(2000);
        String startDate = LocalDateTime.now(zoneUTC).toString(DATE_TIME_FORMAT);

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
                paymentReference = paymentDto.getReference();
                assertNotNull(paymentDto.getReference());
                assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
            });

        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.moneyclaims.service.gov.uk")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
                assertNotNull(paymentDto.getReference());
                assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                String endDate = LocalDateTime.now(zoneUTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

                // retrieve card payment
                dsl.given().userToken(USER_TOKEN)
                    .s2sToken(SERVICE_TOKEN)
                    .when().searchPaymentsBetweenDates(startDate, endDate)
                    .then().getPayments((paymentsResponse -> {
                        assertThat(paymentsResponse.getPayments().size()).isGreaterThanOrEqualTo(2);
                        PaymentDto retrievedPaymentDto = paymentsResponse.getPayments().stream()
                            .filter(o -> o.getPaymentReference().equals(paymentDto.getReference())).findFirst().get();
                        FeeDto feeDto = retrievedPaymentDto.getFees().get(0);
                        assertThat(feeDto.getCode()).isEqualTo("FEE0001");
                        assertThat(feeDto.getVersion()).isEqualTo("1");
                        assertThat(feeDto.getNaturalAccountCode()).isEqualTo("4481102133");
                        assertThat(feeDto.getMemoLine()).isNotEmpty();
                        assertThat(feeDto.getJurisdiction1()).isEqualTo("civil");
                        assertThat(feeDto.getJurisdiction2()).isEqualTo("county court");
                    }));
            });

    }


    private CardPaymentRequest getCardPaymentRequest() {
        return PaymentFixture.aCardPaymentRequest("20.99");
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
