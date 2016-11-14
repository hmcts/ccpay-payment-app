package uk.gov.justice.payment.api.acceptancetests;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.util.ResourceUtils.getFile;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class PaymentsHappyPathTest extends TestBase {
    private static String payment_id = null;

    @Test
    public void test1_POST_Payment_Call() throws IOException {
        payment_id =
                scenario.given().body(getFile("classpath:Messages_Appeal_Post.json"))
                        .when().post()
                        .then().statusCode(CREATED.value())
                        .extract().path("payment_id");
    }

    @Test
    public void test2_GET_Status() {
        scenario.given()
                .when().get(payment_id)
                .then().statusCode(OK.value());

    }

    @Test
    public void test3_GET_Header() {
        scenario.given()
                .when().get(payment_id)
                .then().header("content-type", "application/json;charset=UTF-8");
    }

    @Test
    public void test4_POST_Cancel() {
        scenario.given()
                .when().post(payment_id + "/cancel")
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void test5_GET_Cancel_Status() {
        scenario.given()
                .when().get(payment_id)
                .then()
                .body("state.status", is("cancelled"))
                .body("state.message", is("Payment was cancelled by the service"));
    }
}
