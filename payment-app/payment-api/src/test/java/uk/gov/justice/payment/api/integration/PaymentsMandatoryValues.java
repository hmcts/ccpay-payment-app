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
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsMandatoryValues extends TestBase {

    @Before
    public void setup() throws IOException {

        initialize();

    }

    @Test
    public void test1_POST_Without_Values() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson("", "", "", "", "", "", "");

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(400, r.statusCode());

    }

    @Test
    public void test2_POST_Without_Amount() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson("", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"), CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(400, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("amount_man_msg"), r.path("error"));

    }

    @Test
    public void test3_POST_Without_App_Ref() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson(CONFIG.getProperty("amount"), "", CONFIG.getProperty("description"),
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url"),
                CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(422, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("application_man_msg"), r.body().asString());

    }

    @Test
    public void test4_POST_Without_Desc() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"), "",
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url"),
                CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(422, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("description_man_msg"), r.getBody().asString());

    }

    @Test
    public void test5_POST_Without_Pay_Ref() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), "", CONFIG.getProperty("ret_url"),
                CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(422, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("payment_ref_man_msg"), r.getBody().asString());

    }

    @Test
    public void test6_POST_Without_Ret_Url() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), "",
                CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(422, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("ret_url_man_msg"), r.getBody().asString());

    }

    @Test
    public void test7_POST_Without_Ser_Id() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");


        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"), "");

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                "").contentType("application/json").body(formatted_json).when().post(URL);
        Assert.assertEquals(422, r.statusCode());
        Assert.assertEquals(CONFIG.getProperty("service_id_man_msg"), r.getBody().asString());

    }

    @Test
    public void test8_POST_With_Invalid_Amount() throws IOException {

        String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

        String formatted_json = formatedJson("0", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"), CONFIG.getProperty("service_id"));

        Response r = given().header(CONFIG.getProperty("k_service_id"),
                CONFIG.getProperty("service_id")).contentType("application/json").body(formatted_json).when().post(URL);

        Assert.assertEquals(422, r.statusCode());


    }

    private String formatedJson(String amount, String application_ref, String description, String email,
                                String payment_ref, String ret_url, String service_id) throws IOException {

        Map<String, String> m = new HashMap<String, String>();
        m.put("amount", amount);
        m.put("application_ref", application_ref);
        m.put("description", description);
        m.put("email", email);
        m.put("payment_ref", payment_ref);
        m.put("ret_url", ret_url);
        m.put("service_id", service_id);

        return getProcessedTemplateValue(loadFile("Message_Appeal_Post_Validation.json"), m);
    }

}
