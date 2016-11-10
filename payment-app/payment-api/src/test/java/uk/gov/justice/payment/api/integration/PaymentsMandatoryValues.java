package uk.gov.justice.payment.api.integration;


import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsMandatoryValues extends TestBase {


    @Test
    public void test1_POST_Without_Values() throws IOException {


        String formatted_json = formatedJson("", "", "", "", "", "");

        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    public void test2_POST_Without_Amount() throws IOException {


        String formatted_json = formatedJson("", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"));

        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    public void test3_POST_Without_App_Ref() throws IOException {


        String formatted_json = formatedJson(CONFIG.getProperty("amount"), "", CONFIG.getProperty("description"),
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url")
        );

        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("application_man_msg")));


    }

    @Test
    public void test4_POST_Without_Desc() throws IOException {


        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"), "",
                CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), CONFIG.getProperty("ret_url")
        );
        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("description_man_msg")));

    }

    @Test
    public void test5_POST_Without_Pay_Ref() throws IOException {


        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), "", CONFIG.getProperty("ret_url")
        );
        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("payment_ref_man_msg")));


    }

    @Test
    public void test6_POST_Without_Ret_Url() throws IOException {


        String formatted_json = formatedJson(CONFIG.getProperty("amount"), CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"), ""
        );

        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body(equalTo(CONFIG.getProperty("ret_url_man_msg")));


    }


    @Test
    public void test7_POST_With_Invalid_Amount() throws IOException {


        String formatted_json = formatedJson("-100", CONFIG.getProperty("application_ref"),
                CONFIG.getProperty("description"), CONFIG.getProperty("email"), CONFIG.getProperty("payment_ref"),
                CONFIG.getProperty("ret_url"));

        givenValidRequest()
                .body(formatted_json)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());


    }

    private String formatedJson(String amount, String application_ref, String description, String email,
                                String payment_ref, String ret_url) throws IOException {

        Map<String, String> m = new HashMap<String, String>();
        m.put("amount", amount);
        m.put("application_ref", application_ref);
        m.put("description", description);
        m.put("email", email);
        m.put("payment_ref", payment_ref);
        m.put("ret_url", ret_url);

        return getProcessedTemplateValue(loadFile("Message_Appeal_Post_Validation.json"), m);
    }


}
