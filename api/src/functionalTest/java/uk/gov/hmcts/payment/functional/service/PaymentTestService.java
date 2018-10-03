package uk.gov.hmcts.payment.functional.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.functional.tokens.ServiceTokenFactory;
import uk.gov.hmcts.payment.functional.tokens.UserTokenFactory;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@Scope("prototype")
public class PaymentTestService {

    private final Map<String, String> authHeaders = new HashMap<>();

    @Autowired
    private ServiceTokenFactory serviceTokenFactory;
    @Autowired
    private UserTokenFactory userTokenFactory;

    public String getUserToken(String email, String userId, String password, String role) {
        return userTokenFactory.validTokenForUser(email, userId, password, role);
    }

    public String getServiceToken(String clientId, String serviceSecret) {
        return serviceTokenFactory.validTokenForService(clientId, serviceSecret);
    }

    public Response postcardPayment(String userToken, String serviceToken, CardPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost")
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

    public Response postPbaPayment(String userToken, String serviceToken, CreditAccountPaymentRequest request) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/credit-account-payments");
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

    public RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return RestAssured.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }
}
