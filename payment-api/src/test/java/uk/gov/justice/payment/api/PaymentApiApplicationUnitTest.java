package uk.gov.justice.payment.api;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration
@TestPropertySource(properties = "gov.pay.url=https://publicapi.integration.pymnt.uk/v1/payments")

public class PaymentApiApplicationUnitTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    @Value("${gov.pay.url}")
    private String url;

    @Test
    public void createPayment() {
        String expected = "{  \"amount\":10,  \"state\":{    \"status\":\"created\",    \"finished\":false }}";
        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(expected)
                        .withHeader("Content-Type", "application/json")
                ));

        PaymentController paymentController = new PaymentController();
        paymentController.url = "http://localhost:8089/payments/create";
        assertEquals(paymentController.createPayment(null, null).getStatusCode().value(), 201);


    }


    @Test
    public void viewPayment() {
        String expected = "{   \"amount\":3650,    \"state\":{         \"status\":\"success\",       \"finished\":true    }     }";
        stubFor(get(urlPathMatching("/payments/view/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(expected)
                        .withHeader("Content-Type", "application/json")
                ));

        PaymentController paymentController = new PaymentController();
        paymentController.url = "http://localhost:8089/payments/view";
        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 200);


    }


}
