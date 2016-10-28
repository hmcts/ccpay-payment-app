package uk.gov.justice.payment.api.integration;

import com.jayway.restassured.response.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;


/**
 * Unit test for simple App.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsHappyPath extends TestBase

{

    private static String payment_id = null;


    @Before

    public void setup() throws IOException {

        initialize();

    }

    @Test
    public void test1_POST_Payment_Call() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");
        String myJson = loadFile("Messages_Appeal_Post.json");

        Response r = given().contentType("application/json").body(myJson).when().post(URL);
        Assert.assertEquals(201, r.statusCode());

        if (r.statusCode() == 201) {
            payment_id = r.path("payment_id");
        }

    }

    @Test
    public void test2_GET_Status() {

        String url = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path") + payment_id;

        given().when().get(url).then().assertThat().statusCode(200);

    }

    @Test
    public void test3_GET_Header() {

        String url = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path") + payment_id;
        given().when().get(url).then().assertThat().header("content-type", "application/json;charset=UTF-8");

    }

    @Test
    public void test4_POST_Cancel() {

        String url = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path") + payment_id + CONFIG.getProperty("cancel_path");
        given().when().post(url).then().assertThat().statusCode(204);

    }

    @Test
    public void test5_GET_Cancel_Status() {

        String url = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path") + payment_id;

        Response r = given().when().get(url);
        Assert.assertEquals(CONFIG.getProperty("payment_cancel_status"), r.path("state.status"));
        Assert.assertEquals(CONFIG.getProperty("payment_cancel_message"), r.path("state.message"));

    }


}
