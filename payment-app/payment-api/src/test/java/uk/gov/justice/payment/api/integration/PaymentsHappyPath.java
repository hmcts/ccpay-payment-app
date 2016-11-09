package uk.gov.justice.payment.api.integration;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;


/**
 * Unit test for simple App.
 */

@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsHappyPath extends TestBase {
    private static String payment_id = null;

    @Test
    public void test1_POST_Payment_Call() throws IOException {
        Response response =
                givenValidRequest()
                        .body(loadFile("Messages_Appeal_Post.json"))
                        .when()
                        .post();

        response
                .then()
                .statusCode(201);

        payment_id = response.path("payment_id");
    }

    @Test
    public void test2_GET_Status() {
        givenValidRequest()
                .when()
                .get(payment_id)
                .then()
                .statusCode(200);

    }

    @Test
    public void test3_GET_Header() {
        givenValidRequest()
                .when()
                .get(payment_id)
                .then()
                .header("content-type", "application/json;charset=UTF-8");
    }

    @Test
    public void test4_POST_Cancel() {
        givenValidRequest()
                .when()
                .post(payment_id + "/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    public void test5_GET_Cancel_Status() {
        givenValidRequest()
                .when()
                .get(payment_id)
                .then()
                .body("state.status", is("cancelled"))
                .body("state.message", is("Payment was cancelled by the service"));
    }


}
