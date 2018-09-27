package uk.gov.hmcts.payment.functional.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class PaymentTestService {

    public static Response postcardPayment(String userToken, String serviceToken, CardPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost")
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/card-payments");
    }

    public static Response getCardPayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/card-payments/{reference}", paymentReference);
    }

    public static Response postPbaPayment(String userToken, String serviceToken, CreditAccountPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/credit-account-payments");
    }

    public static Response recordBarPayment(String userToken, String serviceToken, PaymentRecordRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/payment-records");
    }

    public static Response getPbaPayment(String userToken, String serviceToken, String paymentReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/credit-account-payments/{reference}", paymentReference);
    }

    public static RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return RestAssured.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }
}
