package uk.gov.justice.payment.api.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;

import java.util.Calendar;

import static com.jayway.restassured.RestAssured.given;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)

public class HealthCheck {

    public static final int AMOUNT = 10;
    public static final String SERVICE_ID = "divorce";
    private CreatePaymentRequest paymentRequest;
    //private HttpHeaders headers;
    @Autowired
    private TestRestTemplate restTemplate;


    @Before
    public void setUp() {
        long timestamp = Calendar.getInstance().getTimeInMillis();
        RestAssured.port = 8181;
        paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(AMOUNT);
        paymentRequest.setDescription("Test Desc" + timestamp);
        paymentRequest.setPaymentReference("TestRef" + timestamp);
        paymentRequest.setApplicationReference("Test case id" + timestamp);
        paymentRequest.setReturnUrl("https://localhost:8443/payment-result");
        paymentRequest.setEmail("zeeshan.alam@agilesphere.co.uk");
        //headers.put("service-id","divorce");
    }


    @Test
    public void createPayment() {


        setUp();
        given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service-id", SERVICE_ID).
                when().post("/payments").

                then().statusCode(201);
    }


    @Test
    public void viewPayment() {
        setUp();
        String paymentId = given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service-id", SERVICE_ID).
                when().post("/payments").
                then().statusCode(201).extract().path("payment_id");

        given().contentType("application/json").
                when().
                header("service-id", SERVICE_ID).
                get("/payments/" + paymentId).

                then().statusCode(200).extract().path("state.status").equals("created");


    }


    @Test
    public void cancelPayment() {
        setUp();

        String paymentId = given().contentType("application/json").
                body(getJson(paymentRequest)).
                header("service-id", SERVICE_ID).
                when().post("/payments").
                then().statusCode(201).extract().path("payment_id");
        given().contentType("application/json").
                header("service-id", SERVICE_ID).
                when().post("/payments/" + paymentId + "/cancel").
                then().statusCode(204);

    }

    @Test
    public void searchTransactions() {
        setUp();

        given().contentType("application/json").
                when().
                header("service-id", SERVICE_ID).
                get("/payments").
                then().statusCode(200).extract().path("payment_id");
        given().contentType("application/json").
                when().
                header("service-id", SERVICE_ID).
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
