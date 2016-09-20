package uk.gov.justice.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;
import static com.jayway.restassured.RestAssured.given;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration
public class PaymentApiApplicationTest {

    @Test
    public void createPayment() {
        RestAssured.port = 8181;
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("TestDesc");
        paymentRequest.setReference("TestRef");
        paymentRequest.setReturnUrl("https://localhost:8443/payment-result");
        given().contentType("application/json").
                body(getJson(paymentRequest)).
                when().post("/payments").
                then().statusCode(201);
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
