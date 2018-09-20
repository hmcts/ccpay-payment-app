package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PaymentsSearchFunctionalTest extends IntegrationTestBase {

    private static final String DATE_FORMAT_DD_MM_YYYY = "dd-MM-yyyy";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_FORMAT_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";

    @Autowired(required = true)
    private PaymentsTestDsl dsl;

    @Test
    public void searchPaymentsWithDateFormatYYYYMMDDShouldPass() {
        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().getPayments(paymentsResponse -> {
                assertThat(paymentsResponse.getPayments()).isNotNull();
        });
    }

    @Test
    public void searchPaymentWithDateFormatDDMMYYYYShouldPass() {
        String startDate = LocalDate.now().toString(DATE_FORMAT_DD_MM_YYYY);
        String endDate = LocalDate.now().toString(DATE_FORMAT_DD_MM_YYYY);

        dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().getPayments(paymentsResponse -> {
                assertThat(paymentsResponse.getPayments()).isNotNull();
        });
    }

    @Test
    public void searchPaymentsWithoutEndDateShouldFail() {

        String startDate = LocalDateTime.now().toString(DATE_TIME_FORMAT);
        Response response = dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, null)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Both start and end dates are required.");
    }

    @Test
    public void searchPaymentWithFutureDatesShouldFail() {
        String startDate = LocalDateTime.now().toString(DATE_TIME_FORMAT);
        String endDate = LocalDateTime.now().plusMinutes(1).toString(DATE_TIME_FORMAT);

        Response response = dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Date cannot be in the future");
    }

    @Test
    public void searchPaymentWithStartDateGreaterThanEndDateShouldFail() throws Exception {
        String startDate = LocalDateTime.now().plusMinutes(1).toString(DATE_TIME_FORMAT);
        String endDate = LocalDateTime.now().toString(DATE_TIME_FORMAT);

        Response response = dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().validationErrorFor400();

        assertThat(response.getBody().asString()).contains("Start date cannot be greater than end date");
    }

    @Test
    public void searchPaymentsWithStartDateEndDateShouldPass() {
        String startDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT);

        dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret).returnUrl("https://www.google.com")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
            assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });

        dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret).returnUrl("https://www.google.com")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(paymentDto -> {
            assertNotNull(paymentDto.getReference());
            assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
        });

        String endDate = LocalDateTime.now(DateTimeZone.UTC).toString(DATE_TIME_FORMAT_T_HH_MM_SS);

        // retrieve card payment
        dsl.given().userId(paymentCmcTestUser, paymentCmcTestUserId, paymentCmcTestPassword, cmcUserGroup).serviceId(cmcServiceName, cmcSecret)
            .when().searchPaymentsBetweenDates(startDate, endDate)
            .then().getPayments((paymentsResponse -> {
            assertThat(paymentsResponse.getPayments().size()).isEqualTo(2);
        }));

    }


    private String getCardPaymentRequest() {
        int num = new Random().nextInt(100) + 1;
        JSONObject payment = new JSONObject();

        try {
            payment.put("amount", 20.99);
            payment.put("description", "A functional for search payment " + num);
            payment.put("case_reference", "REF_" + num);
            payment.put("service", "CMC");
            payment.put("currency", "GBP");
            payment.put("site_id", "AA0" + num);

            JSONArray fees = new JSONArray();
            JSONObject fee = new JSONObject();
            fee.put("calculated_amount", 20.99);
            fee.put("code", "FEE0" + num);
            fee.put("reference", "REF_" + num);
            fee.put("version", "1");
            fees.put(fee);

            payment.put("fees", fees);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return payment.toString();
    }

}
