package uk.gov.justice.payment.api.acceptancetests;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.http.HttpStatus.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
public class PaymentsMandatoryValuesTest extends TestBase {
    @Test
    public void test1_POST_Without_Values() throws IOException {
        String formatted_json = formattedJson("", "", "", "", "", "");

        scenario.given().body(formatted_json)
                .when().post()
                .then().statusCode(BAD_REQUEST.value());

    }

    @Test
    public void test2_POST_Without_Amount() throws IOException {
        String formatted_json = formattedJson("", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"));

        scenario.given().body(formatted_json)
                .when().post()
                .then().statusCode(BAD_REQUEST.value());

    }

    @Test
    public void test3_POST_Without_App_Ref() throws IOException {
        String formatted_json = formattedJson(CONFIG.getProperty("amount"), "", CONFIG.getProperty("description"),
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url")
        );

        scenario.given().body(formatted_json)
                .when().post()
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("application_man_msg")));
    }

    @Test
    public void test4_POST_Without_Desc() throws IOException {
        String formatted_json = formattedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"), "",
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url")
        );

        scenario.given().body(formatted_json)
                .when().post()
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("description_man_msg")));
    }

    @Test
    public void test5_POST_Without_Pay_Ref() throws IOException {
        String formatted_json = formattedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), "", CONFIG.getProperty("ret_url")
        );
        scenario.given().body(formatted_json)
                .when().post()
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("payment_ref_man_msg")));
    }

    @Test
    public void test6_POST_Without_Ret_Url() throws IOException {
        String formatted_json = formattedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), ""
        );

        scenario.given().body(formatted_json)
                .when().post()
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("ret_url_man_msg")));
    }


    @Test
    public void test7_POST_With_Invalid_Amount() throws IOException {
        String formatted_json = formattedJson("-100", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"));

        scenario.given().body(formatted_json)
                .when().post()
                .then().statusCode(UNPROCESSABLE_ENTITY.value());
    }

    private String formattedJson(String amount, String application_ref, String description, String email,
                                 String payment_ref, String ret_url) throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        m.put("amount", amount);
        m.put("application_ref", application_ref);
        m.put("description", description);
        m.put("email", email);
        m.put("payment_ref", payment_ref);
        m.put("ret_url", ret_url);

        return getProcessedTemplateValue("Message_Appeal_Post_Validation.json", m);
    }
}
