package uk.gov.justice.payment.api.acceptancetests;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.util.ResourceUtils.getFile;

@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsHappyPathTest extends TestBase {
    private static String payment_id = null;

    @Test
    public void test1_POST_Payment_Call() throws IOException {

        String formatted_json = formatedJson(appRefNumber("test-app"));
        payment_id =
                scenario.given().body(formatted_json)
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
                .body("state.status", is("cancelled"));
    }

    private String formatedJson(String application_ref)
            throws IOException {

        Map<String, String> m = new HashMap<String, String>();

        m.put("application_ref", application_ref);
        return getProcessedTemplateValue("Messages_Appeal_Post.json", m);
    }

    private static String appRefNumber(String appName) throws IOException {

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        String dateString = formatter.format(date);
        String appRefUpdatedName = appName + "_" + dateString;
        return appRefUpdatedName;
    }
}
