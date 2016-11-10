package uk.gov.justice.payment.api.integration;

import io.restassured.response.Response;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.Matchers.is;



@RunWith(SpringRunner.class)
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
                .statusCode(HttpStatus.CREATED.value());

        payment_id = response.path("payment_id");
    }

    @Test
    public void test2_GET_Status() {
        givenValidRequest()
                .when()
                .get(payment_id)
                .then()
                .statusCode(HttpStatus.OK.value());

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
                .statusCode(HttpStatus.NO_CONTENT.value());
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
