package uk.gov.hmcts.payment.functional.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;

import javax.inject.Named;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Named
public class CaseTestService {



    public Response getPaymentGroupsForCase(final String userToken,
                                            final String serviceToken,
                                            final String ccdCaseNumber) {
        RestAssured.defaultParser = Parser.JSON;

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
}
