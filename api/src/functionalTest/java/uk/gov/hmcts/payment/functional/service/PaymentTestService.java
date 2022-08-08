package uk.gov.hmcts.payment.functional.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.lessThan;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class PaymentTestService {

    private final Map<String, String> authHeaders = new HashMap<>();

    public Response postcardPayment(String userToken, String serviceToken, CardPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost.hmcts.net")
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/card-payments");
    }

    public Response getCardPayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/card-payments/{reference}", paymentReference);
    }

    public Response getPayments(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/payments/{reference}", paymentReference);
    }

    public Response postPbaPayment(String userToken, String serviceToken, CreditAccountPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/credit-account-payments");
    }

    public Response postInitiateRefund(String userToken, String serviceToken,
                                       PaymentRefundRequest paymentRefundRequest) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(paymentRefundRequest)
            .when()
            .post("/refund-for-payment");
    }

    public Response postSubmitRefund(String userToken, String serviceToken,
                                     RetrospectiveRemissionRequest retrospectiveRemissionRequest) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(retrospectiveRemissionRequest)
            .when()
            .post("/refund-retro-remission");
    }

    public Response updateThePaymentDateByCcdCaseNumberForCertainHours(final String userToken,
                                                                       final String serviceToken,
                                                                       final String ccdCaseNumber,
                                                                       final String lagTime) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .patch("/payments/ccd_case_reference/{ccd_case_number}/lag_time/{lag_time}", ccdCaseNumber, lagTime);
    }

    public Response recordBarPayment(String userToken, String serviceToken, PaymentRecordRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/payment-records");
    }

    public Response getPbaPayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/credit-account-payments/{reference}", paymentReference);
    }

    public Response getPbaPaymentsByAccountNumber(String userToken, String serviceToken, String accountNumber) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/pba-accounts/{accountNumber}/payments", accountNumber);
    }

    public Response getPbaPaymentsByCCDCaseNumber(String serviceToken, String ccdCaseNumber) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?ccd_case_number={ccdCaseNumber}", ccdCaseNumber);
    }

    public Response getPbaPaymentsByCCDCaseNumberApproach1(String serviceToken, String ccdCaseNumber) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?ccd_case_number={ccdCaseNumber}", ccdCaseNumber);
    }

    public ValidatableResponse getLiberatePullPaymentsByStartAndEndDate(String serviceToken, String startDate, String endDate,
                                                                        Long responseTime) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?end_date={endDate}&start_date={startDate}", endDate,startDate)
            .then().time(lessThan(responseTime),TimeUnit.SECONDS);
    }

    public ValidatableResponse getLiberatePullPaymentsByStartAndEndDateApproach1(String serviceToken, String startDate,
                                                                                 String endDate, Long responseTime) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?end_date={endDate}&start_date={startDate}", endDate, startDate)
            .then().time(lessThan(responseTime), TimeUnit.SECONDS);
    }


    public Response getLiberatePullPaymentsTimeByStartAndEndDate(String serviceToken, String startDate, String endDate) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?end_date={endDate}&start_date={startDate}", endDate,startDate);

    }

    public Response getLiberatePullPaymentsTimeByStartAndEndDateApproach1(String serviceToken, String startDate, String endDate) {
        return givenWithServiceHeaders(serviceToken)
            .when()
            .get("/reconciliation-payments?end_date={endDate}&start_date={startDate}", endDate, startDate);

    }

    public Response getPaymentGroupsForCase(final String userToken,
                                            final String serviceToken,
                                            final String ccdCaseNumber) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .when()
            .get("/cases/{ccdcasenumber}/paymentgroups",ccdCaseNumber);
    }

    public RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return SerenityRest.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }

    public RequestSpecification givenWithServiceHeaders(String serviceToken) {
        return SerenityRest.given()
            .header("ServiceAuthorization", serviceToken);
    }

    public Response deletePayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
                .when()
                .delete("/credit-account-payments/{paymentReference}", paymentReference);
    }

    public Response deleteRefund(final String userToken, final String serviceToken,
                                 final String refundReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
                .when()
                .delete("/refund/{reference}", refundReference);
    }

    public Response postBounceCheque(String serviceToken,
                                       PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) {
        return givenWithServiceHeaders(serviceToken)
            .contentType(ContentType.JSON)
            .body(paymentStatusBouncedChequeDto)
            .when()
            .post("/payment-failures/bounced-cheque");
    }

    public Response postChargeback(String serviceToken,
                                     PaymentStatusChargebackDto paymentStatusChargebackDto) {
        return givenWithServiceHeaders(serviceToken)
            .contentType(ContentType.JSON)
            .body(paymentStatusChargebackDto)
            .when()
            .post("/payment-failures/chargeback");
    }

    public Response getFailurePayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/payment-failures/{paymentReference}", paymentReference);
    }

    public Response deleteFailedPayment(String userToken, String serviceToken, String failureReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .delete("/payment-status-delete/{failureReference}", failureReference);
    }

    public Response paymentStatusSecond(String serviceToken, String failureReference,
                                        PaymentStatusUpdateSecond paymentStatusUpdateSecond) {
        return givenWithServiceHeaders(serviceToken)
                .contentType(ContentType.JSON)
                .body(paymentStatusUpdateSecond)
                .when()
                .patch("/payment-failures/{failureReference}", failureReference);
    }
}
