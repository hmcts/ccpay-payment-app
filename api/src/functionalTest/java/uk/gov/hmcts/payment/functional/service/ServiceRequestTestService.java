package uk.gov.hmcts.payment.functional.service;


import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Named
public class ServiceRequestTestService {

    private final Map<String, String> authHeaders = new HashMap<>();

    public Response createServiceRequest(String userToken,
                                         String serviceToken,
                                         ServiceRequestDto serviceRequestDto) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost.hmcts.net")
            .contentType(ContentType.JSON)
            .body(serviceRequestDto)
            .when()
            .post("/service-request");
    }

    public Response getPaymentGroups(String userToken,
                                     String serviceToken,
                                     final String ccdCaseNumber) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost.hmcts.net")
            .contentType(ContentType.JSON)
            .when()
            .get("/cases/{ccdcasenumber}/paymentgroups", ccdCaseNumber);
    }

    public Response createPBAPaymentForAServiceRequest(final String userToken,
                                                       final String serviceToken,
                                                       final String idempotencyKey,
                                                       final String serviceRequestReference,
                                                       final ServiceRequestPaymentDto serviceRequestPaymentDto) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("idempotency_key", idempotencyKey)
            .contentType(ContentType.JSON)
            .body(serviceRequestPaymentDto)
            .when()
            .post("/service-request/{serviceRequestReference}/pba-payments", serviceRequestReference);
    }

    public Response createAnOnlineCardPaymentForAServiceRequest(final String userToken,
                                                                final String serviceToken,
                                                                final String serviceRequestReference,
                                                                final OnlineCardPaymentRequest onlineCardPaymentRequest) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost.hmcts.net")
            .contentType(ContentType.JSON)
            .body(onlineCardPaymentRequest)
            .when()
            .post("/service-request/{service-request-reference}/card-payments", serviceRequestReference);
    }

    public RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return RestAssured.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }

    public RequestSpecification givenWithServiceHeaders(String serviceToken) {
        return RestAssured.given()
            .header("ServiceAuthorization", serviceToken);
    }
}
