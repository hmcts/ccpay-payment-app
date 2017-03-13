package uk.gov.justice.payment.api.acceptancetests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)

public class HealthCheck {

    public static final int AMOUNT = 10;
    public static final String SERVICE_ID = "divorce";
    private Map<String, Object> paymentRequest;
    //private HttpHeaders headers;
    @Autowired
    private TestRestTemplate restTemplate;


    @Before
    public void setUp() {
        long timestamp = Calendar.getInstance().getTimeInMillis();
        RestAssured.port = 8080;
        paymentRequest = new HashMap();
        paymentRequest.put("amount", AMOUNT);
        paymentRequest.put("description", "Test Desc" + timestamp);
        paymentRequest.put("payment_reference", "TestRef" + timestamp);
        paymentRequest.put("application_reference", "Test case id" + timestamp);
        paymentRequest.put("return_url", "https://localhost:8443/payment-result");
        paymentRequest.put("email", "zeeshan.alam@agilesphere.co.uk");
    }


    @Test
    public void createPayment() {


        setUp();
        given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service_id", SERVICE_ID).
                when().post("/payments").

                then().statusCode(201);
    }


    @Test
    public void viewPayment() {
        setUp();
        String paymentId = given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service_id", SERVICE_ID).
                when().post("/payments").
                then().statusCode(201).extract().path("payment_id");

        given().contentType("application/json").
                when().
                header("service_id", SERVICE_ID).
                get("/payments/" + paymentId).

                then().statusCode(200).extract().path("state.status").equals("created");


    }


    @Test
    public void cancelPayment() {
        setUp();

        String paymentId = given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service_id", SERVICE_ID).
                when().post("/payments").
                then().statusCode(201).extract().path("payment_id");
        given().contentType("application/json").
                header("service_id", SERVICE_ID).
                when().post("/payments/" + paymentId + "/cancel").
                then().statusCode(204);

    }

    @Test
    public void searchTransactions() {
        setUp();

        given().contentType("application/json").
                when().
                header("service_id", SERVICE_ID).
                get("/payments").
                then().statusCode(200).extract().path("payment_id");
        given().contentType("application/json").
                when().
                header("service_id", SERVICE_ID).
                get("/payments/?amount=" + AMOUNT).
                then().statusCode(200);

    }

    private String getJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
