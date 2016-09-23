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
import uk.gov.justice.payment.api.json.CreatePaymentRequestInternal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@Configuration
//@TestPropertySource(properties = "gov.pay.url=https://publicapi.integration.pymnt.uk/v1/payments")

public class PaymentApiApplicationUnitTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    //PaymentController paymentController = new PaymentController(new RestTemplate());
    PaymentController paymentController = new PaymentController();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    private static String createPaymentExpected = "{       \"amount\":2000,     \"state\":{          \"status\":\"created\",        \"finished\":false     },     \"description\":\"Fee for test\",     \"reference\":\"REFTEST\",     \"payment_id\":\"j45g29fhr75s134jv7upob95jm\",     \"payment_provider\":\"sandbox\",     \"return_url\":\"https://localhost:8443/payment-result\",     \"created_date\":\"2016-09-21T15:08:48.148Z\",     \"refund_summary\":{          \"status\":\"pending\",        \"amount_available\":2000,        \"amount_submitted\":0     },     \"_links\":{          \"self\":{             \"href\":\"https://publicapi.integration.pymnt.uk/v1/payments/j45g29fhr75s134jv7upob95jm\",           \"method\":\"GET\"        },        \"next_url\":{             \"href\":\"https://www.integration.pymnt.uk/secure/a663dae7-d00e-4c02-885a-71d1e022192c\",           \"method\":\"GET\"        },        \"next_url_post\":{             \"type\":\"application/x-www-form-urlencoded\",           \"params\":{                \"chargeTokenId\":\"a663dae7-d00e-4c02-885a-71d1e022192c\"           },           \"href\":\"https://www.integration.pymnt.uk/secure\",           \"method\":\"POST\"        },        \"events\":{             \"href\":\"https://publicapi.integration.pymnt.uk/v1/payments/j45g29fhr75s134jv7upob95jm/events\",           \"method\":\"GET\"        },        \"refunds\":{             \"href\":\"https://publicapi.integration.pymnt.uk/v1/payments/j45g29fhr75s134jv7upob95jm/refunds\",           \"method\":\"GET\"        },        \"cancel\":{             \"href\":\"https://publicapi.integration.pymnt.uk/v1/payments/j45g29fhr75s134jv7upob95jm/cancel\",           \"method\":\"POST\"        }     },     \"card_brand\":\"\"  }";
    private static String viewPaymentExpected = "{   \"amount\":3650,    \"state\":{         \"status\":\"success\",       \"finished\":true    }     }";
    private CreatePaymentRequestInternal paymentRequest;
    @Before
    public void setUp() {
        ReflectionTestUtils.setField(paymentController,"restTemplate",restTemplate);
        ReflectionTestUtils.setField(paymentController,"mapper",mapper);
        paymentRequest = new CreatePaymentRequestInternal();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("Test Desc");
        paymentRequest.setPaymentReference("TestRef");
        paymentRequest.setApplicationReference("TEST_SERVICE");
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
        assertEquals(paymentController.createPayment(paymentRequest).getStatusCode().value(), 201);
    }

    @Test
    public void createPaymentAuthenticationFailure() {
        stubFor(post(urlPathMatching("/payments/create"))
                .willReturn(aResponse()
                        .withStatus(401)

                ));
        ReflectionTestUtils.setField(paymentController,"url","http://localhost:8089/payments/create");
        assertEquals(paymentController.createPayment(paymentRequest).getStatusCode().value(), 401);
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
        assertEquals(paymentController.viewPayment(null).getStatusCode().value(), 200);
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
        assertEquals(paymentController.viewPayment(null).getStatusCode().value(), 404);
    }
}
