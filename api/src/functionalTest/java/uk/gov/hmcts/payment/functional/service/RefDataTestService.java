package uk.gov.hmcts.payment.functional.service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class RefDataTestService {


    public static final Response postOrganisation(final String serviceToken, final String URI, final String requestBody) {
        return givenWithServiceHeaders(serviceToken)
            .baseUri(URI)
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/refdata/internal/v1/organisations");
    }

    public static final Response approveOrganisation(final String authToken,
                                                     final String serviceToken,
                                                     final String URI,
                                                     final String payload,
                                                     final String organisationIdentifier) {
        return givenWithAuthHeaders(authToken, serviceToken)
            .baseUri(URI)
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .put("/refdata/internal/v1/organisations/{organisationIdentifier}", organisationIdentifier);
    }

    public static String readFileContents(final String path) throws IOException {

        File file = ResourceUtils.getFile("classpath:" + path);
        //File is found
        return new String(Files.readAllBytes(Paths.get(file.toURI())));
    }


    public static final RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return RestAssured.given()
            .header(AUTHORIZATION, userToken)
            .header("ServiceAuthorization", serviceToken);
    }

    public static final RequestSpecification givenWithServiceHeaders(final String serviceToken) {
        return RestAssured.given()
            .header("ServiceAuthorization", serviceToken);
    }
}
