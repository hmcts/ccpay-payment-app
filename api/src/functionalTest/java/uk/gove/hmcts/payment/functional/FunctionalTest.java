package uk.gove.hmcts.payment.functional;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.functional.IntegrationTestBase;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class FunctionalTest extends IntegrationTestBase {

    @Value("${test.url:http://localhost:8080}")
    private String testUrl;

    @Before
    public void setup() {
        RestAssured.baseURI = testUrl;
    }

    @Test
    public void shouldReturnChannels() {
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/refdata/channels")
            .then()
            .statusCode(200);
    }
}
