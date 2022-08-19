package uk.gov.hmcts.payment.functional.service;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class PBAAccountsTestService {


    public static final Response getPBAAccounts(final String userToken,
                                                final String serviceToken) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .header("return-url", "http://localhost.hmcts.net")
            .contentType(ContentType.JSON)
            .when()
            .get("/pba-accounts");
    }

    public static final RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return SerenityRest.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }

    public static final RequestSpecification givenWithServiceHeaders(final String serviceToken) {
        return SerenityRest.given()
            .header("ServiceAuthorization", serviceToken);
    }
}
