package uk.gov.justice.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration
@TestPropertySource(properties = "gov.pay.url=https://publicapi.integration.pymnt.uk/v1/payments")

public class PaymentApiApplicationUnitTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    //PaymentController paymentController = new PaymentController(new RestTemplate());
    PaymentController paymentController = new PaymentController();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    private static String createPaymentExpected = "{  \"amount\":10,  \"state\":{    \"status\":\"created\",    \"finished\":false }}";
    private static String viewPaymentExpected = "{   \"amount\":3650,    \"state\":{         \"status\":\"success\",       \"finished\":true    }     }";

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);
    }
    @Test
    public void createPaymentSuccess() {
        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(createPaymentExpected)
                        .withHeader("Content-Type", "application/json")
                ));

        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/create");
        assertEquals(paymentController.createPayment(null, null).getStatusCode().value(), 201);
    }

    @Test
    public void createPaymentAuthenticationFailure() {
        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(401)

                ));
        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/create");
        assertEquals(paymentController.createPayment(null, null).getStatusCode().value(), 401);
    }


    @Test
    public void viewPayment() {
        stubFor(get(urlPathMatching("/payments/view/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(viewPaymentExpected)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/view");
        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 200);
   }

//    @Test
//    public void viewPaymentNoResponse() {
//        stubFor(get(urlPathMatching("/payments/view/.*"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withBody("bad data")
//                        .withHeader("Content-Type", "application/json")
//                ));
//        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/view");
//        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 200);
//    }

    @Test
    public void paymentNotFound() {
        stubFor(get(urlPathMatching("/payments/view/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody(viewPaymentExpected)
                        .withHeader("Content-Type", "application/json")
                ));
        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/view");
        assertEquals(paymentController.viewPayment(null, null).getStatusCode().value(), 404);
    }
}
